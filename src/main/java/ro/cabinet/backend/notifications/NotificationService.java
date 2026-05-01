package ro.cabinet.backend.notifications;

import ro.cabinet.backend.notifications.client.MessaggioSmsClient;
import ro.cabinet.backend.notifications.client.ProviderSendResult;
import ro.cabinet.backend.notifications.client.WahaClient;
import ro.cabinet.backend.notifications.client.WahaSessionInfo;
import ro.cabinet.backend.notifications.dto.ReminderDispatchRequest;
import ro.cabinet.backend.notifications.dto.ReminderDispatchResponse;
import ro.cabinet.backend.notifications.entity.ClinicNotificationSettings;
import ro.cabinet.backend.notifications.entity.NotificationAttempt;
import ro.cabinet.backend.notifications.entity.NotificationOutbox;
import ro.cabinet.backend.notifications.repo.NotificationAttemptRepository;
import ro.cabinet.backend.notifications.repo.NotificationOutboxRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationOutboxRepository outboxRepository;
  private final NotificationAttemptRepository attemptRepository;
  private final ClinicNotificationSettingsService settingsService;
  private final WahaClient wahaClient;
  private final MessaggioSmsClient messaggioSmsClient;

  public NotificationService(NotificationOutboxRepository outboxRepository,
                             NotificationAttemptRepository attemptRepository,
                             ClinicNotificationSettingsService settingsService,
                             WahaClient wahaClient,
                             MessaggioSmsClient messaggioSmsClient) {
    this.outboxRepository = outboxRepository;
    this.attemptRepository = attemptRepository;
    this.settingsService = settingsService;
    this.wahaClient = wahaClient;
    this.messaggioSmsClient = messaggioSmsClient;
  }

  @Transactional
  public ReminderDispatchResponse dispatchReminder(ReminderDispatchRequest request) {
    validateRequest(request);
    NotificationOutbox existing = findDuplicate(request);
    if (existing != null) {
      return toResponse(existing);
    }

    NotificationPreference preference = request.getPreference() == null
        ? settingsService.resolveDefaultPreference(request.getClinicId())
        : request.getPreference();
    String sessionName = settingsService.resolveWahaSessionName(request.getClinicId());
    boolean smsFallbackEnabled = settingsService.isSmsFallbackEnabled(request.getClinicId());
    NotificationOutbox outbox = outboxRepository.save(createPendingOutbox(request, preference));
    boolean allowRequestFallback = request.getFallbackToSms() == null || request.getFallbackToSms();

    if (preference == NotificationPreference.SMS_ONLY) {
      ProviderSendResult smsResult = sendSms(outbox);
      if (smsResult.isSuccess()) {
        markSent(outbox, NotificationChannel.SMS, false, smsResult.getProviderMessageId(), null, NotificationStatus.SENT);
      } else {
        markFailed(outbox, smsResult.getErrorMessage(), NotificationChannel.SMS);
      }
      return toResponse(outboxRepository.save(outbox));
    }

    if (preference == NotificationPreference.WHATSAPP_ONLY) {
      ProviderSendResult whatsappResult = sendWhatsappIfPossible(outbox, request.getClinicId(), sessionName);
      if (whatsappResult.isSuccess()) {
        markSent(outbox, NotificationChannel.WHATSAPP, false, whatsappResult.getProviderMessageId(), null, NotificationStatus.SENT);
      } else {
        markFailed(outbox, whatsappResult.getErrorMessage(), NotificationChannel.WHATSAPP);
      }
      return toResponse(outboxRepository.save(outbox));
    }

    ProviderSendResult whatsappResult = sendWhatsappIfPossible(outbox, request.getClinicId(), sessionName);
    if (whatsappResult.isSuccess()) {
      markSent(outbox, NotificationChannel.WHATSAPP, false, whatsappResult.getProviderMessageId(), null, NotificationStatus.SENT);
      return toResponse(outboxRepository.save(outbox));
    }

    if (allowRequestFallback && smsFallbackEnabled) {
      ProviderSendResult smsResult = sendSms(outbox);
      if (smsResult.isSuccess()) {
        markSent(outbox, NotificationChannel.SMS, true, smsResult.getProviderMessageId(),
            whatsappResult.getErrorMessage(), NotificationStatus.FALLBACK_SENT);
      } else {
        String combinedError = mergeErrors(whatsappResult.getErrorMessage(), smsResult.getErrorMessage());
        markFailed(outbox, combinedError, NotificationChannel.SMS);
      }
    } else {
      markFailed(outbox, whatsappResult.getErrorMessage(), NotificationChannel.WHATSAPP);
    }
    return toResponse(outboxRepository.save(outbox));
  }

  @Transactional(readOnly = true)
  public List<ReminderDispatchResponse> getByAppointmentExternalId(String appointmentExternalId) {
    if (appointmentExternalId == null || appointmentExternalId.trim().isEmpty()) {
      throw new NotificationValidationException("appointmentExternalId is required");
    }
    return outboxRepository.findTop20ByAppointmentExternalIdOrderByCreatedAtDesc(appointmentExternalId.trim()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<NotificationOutbox> findOutboxByAppointmentExternalId(String appointmentExternalId) {
    if (appointmentExternalId == null || appointmentExternalId.trim().isEmpty()) {
      throw new NotificationValidationException("appointmentExternalId is required");
    }
    return outboxRepository.findTop20ByAppointmentExternalIdOrderByCreatedAtDesc(appointmentExternalId.trim());
  }

  private NotificationOutbox findDuplicate(ReminderDispatchRequest request) {
    String appointmentExternalId = trimToNull(request.getAppointmentExternalId());
    String patientId = trimToNull(request.getPatientId());
    String messageText = trimToNull(request.getMessageText());
    if (appointmentExternalId == null || patientId == null || messageText == null) {
      return null;
    }
    List<NotificationOutbox> duplicates = outboxRepository.findRecentDuplicates(
        appointmentExternalId,
        patientId,
        messageText,
        Set.of(NotificationStatus.SENT, NotificationStatus.FALLBACK_SENT),
        OffsetDateTime.now().minusHours(24));
    return duplicates.isEmpty() ? null : duplicates.get(0);
  }

  private NotificationOutbox createPendingOutbox(ReminderDispatchRequest request, NotificationPreference preference) {
    NotificationOutbox outbox = new NotificationOutbox();
    outbox.setClinicId(request.getClinicId());
    outbox.setDoctorId(request.getDoctorId());
    outbox.setPatientId(trimToNull(request.getPatientId()));
    outbox.setPatientName(request.getPatientName().trim());
    outbox.setPhoneE164(request.getPhoneE164().trim());
    outbox.setAppointmentExternalId(trimToNull(request.getAppointmentExternalId()));
    outbox.setAppointmentDateTime(request.getAppointmentDateTime());
    outbox.setMessageText(request.getMessageText().trim());
    outbox.setPreference(preference);
    outbox.setStatus(NotificationStatus.PENDING);
    outbox.setFallbackUsed(false);
    return outbox;
  }

  private ProviderSendResult sendWhatsappIfPossible(NotificationOutbox outbox, UUID clinicId, String sessionName) {
    ClinicNotificationSettings settings = settingsService.getOrCreateSettings(clinicId);
    if (!settings.isWhatsappEnabled()) {
      String message = "WhatsApp is disabled for clinic.";
      saveAttempt(outbox, NotificationChannel.WHATSAPP, NotificationStatus.FAILED, null, message);
      return ProviderSendResult.failure(message);
    }
    WahaSessionInfo status = wahaClient.getSessionStatus(sessionName);
    if (status.getStatus() != WahaSessionStatus.WORKING) {
      String message = "WAHA session not ready: " + status.getStatus()
          + (status.getMessage() == null ? "" : " (" + status.getMessage() + ")");
      saveAttempt(outbox, NotificationChannel.WHATSAPP, NotificationStatus.FAILED, null, message);
      return ProviderSendResult.failure(message);
    }
    ProviderSendResult result = wahaClient.sendText(sessionName, outbox.getPhoneE164(), outbox.getMessageText());
    saveAttempt(outbox, NotificationChannel.WHATSAPP,
        result.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED,
        result.getProviderMessageId(), result.getErrorMessage());
    return result;
  }

  private ProviderSendResult sendSms(NotificationOutbox outbox) {
    ProviderSendResult result = messaggioSmsClient.send(
        outbox.getPhoneE164(),
        outbox.getMessageText(),
        outbox.getAppointmentExternalId() == null ? outbox.getId().toString() : outbox.getAppointmentExternalId());
    saveAttempt(outbox, NotificationChannel.SMS,
        result.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED,
        result.getProviderMessageId(), result.getErrorMessage());
    return result;
  }

  private void saveAttempt(NotificationOutbox outbox, NotificationChannel channel, NotificationStatus status,
                           String providerMessageId, String errorMessage) {
    NotificationAttempt attempt = new NotificationAttempt();
    attempt.setNotificationOutbox(outbox);
    attempt.setChannel(channel);
    attempt.setStatus(status);
    attempt.setProviderMessageId(trimToNull(providerMessageId));
    attempt.setErrorMessage(trimToNull(errorMessage));
    attemptRepository.save(attempt);
  }

  private void markSent(NotificationOutbox outbox, NotificationChannel channel, boolean fallbackUsed,
                        String providerMessageId, String lastError, NotificationStatus status) {
    outbox.setStatus(status);
    outbox.setChannelUsed(channel);
    outbox.setFallbackUsed(fallbackUsed);
    outbox.setProviderMessageId(trimToNull(providerMessageId));
    outbox.setLastError(trimToNull(lastError));
  }

  private void markFailed(NotificationOutbox outbox, String errorMessage, NotificationChannel attemptedChannel) {
    outbox.setStatus(NotificationStatus.FAILED);
    outbox.setChannelUsed(attemptedChannel);
    outbox.setFallbackUsed(false);
    outbox.setLastError(trimToNull(errorMessage));
  }

  private ReminderDispatchResponse toResponse(NotificationOutbox outbox) {
    ReminderDispatchResponse response = new ReminderDispatchResponse();
    response.setNotificationId(outbox.getId());
    response.setStatus(outbox.getStatus());
    response.setChannelUsed(outbox.getChannelUsed());
    response.setFallbackUsed(outbox.isFallbackUsed());
    response.setProviderMessageId(outbox.getProviderMessageId());
    response.setErrorMessage(outbox.getLastError());
    return response;
  }

  private void validateRequest(ReminderDispatchRequest request) {
    if (request == null) {
      throw new NotificationValidationException("Request is required");
    }
    if (request.getClinicId() == null) {
      throw new NotificationValidationException("clinicId is required");
    }
    if (trimToNull(request.getPatientName()) == null) {
      throw new NotificationValidationException("patientName is required");
    }
    if (trimToNull(request.getPhoneE164()) == null) {
      throw new NotificationValidationException("phoneE164 is required");
    }
    if (trimToNull(request.getMessageText()) == null) {
      throw new NotificationValidationException("messageText is required");
    }
  }

  private static String mergeErrors(String first, String second) {
    String left = trimToNull(first);
    String right = trimToNull(second);
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return left + " | " + right;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
