package ro.cabinet.backend.notifications;

import ro.cabinet.backend.notifications.client.WahaClient;
import ro.cabinet.backend.notifications.client.WahaQrData;
import ro.cabinet.backend.notifications.client.WahaSessionInfo;
import ro.cabinet.backend.notifications.dto.ClinicNotificationSettingsRequest;
import ro.cabinet.backend.notifications.dto.ClinicNotificationSettingsResponse;
import ro.cabinet.backend.notifications.dto.WahaQrResponse;
import ro.cabinet.backend.notifications.dto.WahaSessionResponse;
import ro.cabinet.backend.notifications.entity.ClinicNotificationSettings;
import ro.cabinet.backend.notifications.repo.ClinicNotificationSettingsRepository;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicNotificationSettingsService {
  private final ClinicNotificationSettingsRepository repository;
  private final WahaClient wahaClient;

  public ClinicNotificationSettingsService(ClinicNotificationSettingsRepository repository,
                                           WahaClient wahaClient) {
    this.repository = repository;
    this.wahaClient = wahaClient;
  }

  @Transactional
  public ClinicNotificationSettings getOrCreateSettings(UUID clinicId) {
    if (clinicId == null) {
      throw new NotificationValidationException("clinicId is required");
    }
    return repository.findByClinicId(clinicId).orElseGet(() -> repository.save(createDefaultSettings(clinicId)));
  }

  @Transactional
  public ClinicNotificationSettingsResponse updateSettings(ClinicNotificationSettingsRequest request) {
    if (request == null || request.getClinicId() == null) {
      throw new NotificationValidationException("clinicId is required");
    }
    ClinicNotificationSettings settings = getOrCreateSettings(request.getClinicId());
    if (request.getWhatsappEnabled() != null) {
      settings.setWhatsappEnabled(request.getWhatsappEnabled());
    }
    if (request.getSmsFallbackEnabled() != null) {
      settings.setSmsFallbackEnabled(request.getSmsFallbackEnabled());
    }
    if (request.getDefaultPreference() != null) {
      settings.setDefaultPreference(request.getDefaultPreference());
    }
    if (request.getWahaSessionName() != null) {
      String trimmed = request.getWahaSessionName().trim();
      settings.setWahaSessionName(trimmed.isEmpty() ? defaultSessionName(settings.getClinicId()) : trimmed);
    }
    ClinicNotificationSettings saved = repository.save(settings);
    return toResponse(saved);
  }

  @Transactional
  public String resolveWahaSessionName(UUID clinicId) {
    ClinicNotificationSettings settings = getOrCreateSettings(clinicId);
    String sessionName = settings.getWahaSessionName();
    if (sessionName == null || sessionName.trim().isEmpty()) {
      return defaultSessionName(clinicId);
    }
    return sessionName.trim();
  }

  @Transactional
  public NotificationPreference resolveDefaultPreference(UUID clinicId) {
    return getOrCreateSettings(clinicId).getDefaultPreference();
  }

  @Transactional
  public boolean isSmsFallbackEnabled(UUID clinicId) {
    return getOrCreateSettings(clinicId).isSmsFallbackEnabled();
  }

  @Transactional
  public ClinicNotificationSettingsResponse getSettingsResponse(UUID clinicId) {
    return toResponse(getOrCreateSettings(clinicId));
  }

  @Transactional
  public WahaSessionResponse getWahaSessionStatus(UUID clinicId) {
    String sessionName = resolveWahaSessionName(clinicId);
    WahaSessionInfo info = wahaClient.getSessionStatus(sessionName);
    WahaSessionResponse response = new WahaSessionResponse();
    response.setSessionName(info.getSessionName());
    response.setStatus(info.getStatus());
    response.setMessage(info.getMessage());
    return response;
  }

  @Transactional
  public WahaSessionResponse createOrStartWahaSession(UUID clinicId) {
    String sessionName = resolveWahaSessionName(clinicId);
    WahaSessionInfo info = wahaClient.createOrStartSession(sessionName);
    WahaSessionResponse response = new WahaSessionResponse();
    response.setSessionName(info.getSessionName());
    response.setStatus(info.getStatus());
    response.setMessage(info.getMessage());
    return response;
  }

  @Transactional
  public WahaQrResponse getWahaQr(UUID clinicId) {
    String sessionName = resolveWahaSessionName(clinicId);
    WahaQrData data = wahaClient.getQr(sessionName);
    WahaQrResponse response = new WahaQrResponse();
    response.setSessionName(data.getSessionName());
    response.setQrCode(data.getQrCode());
    response.setQrImageBase64(data.getQrImageBase64());
    response.setStatus(data.getStatus());
    response.setMessage(data.getMessage());
    return response;
  }

  private ClinicNotificationSettingsResponse toResponse(ClinicNotificationSettings settings) {
    ClinicNotificationSettingsResponse response = new ClinicNotificationSettingsResponse();
    response.setClinicId(settings.getClinicId());
    response.setWhatsappEnabled(settings.isWhatsappEnabled());
    response.setWahaSessionName(settings.getWahaSessionName());
    response.setSmsFallbackEnabled(settings.isSmsFallbackEnabled());
    response.setDefaultPreference(settings.getDefaultPreference());
    WahaSessionInfo sessionInfo = wahaClient.getSessionStatus(settings.getWahaSessionName());
    response.setWahaSessionStatus(sessionInfo.getStatus());
    WahaQrData qrData = wahaClient.getQr(settings.getWahaSessionName());
    response.setQrAvailable((qrData.getQrCode() != null && !qrData.getQrCode().isBlank())
        || (qrData.getQrImageBase64() != null && !qrData.getQrImageBase64().isBlank()));
    return response;
  }

  private ClinicNotificationSettings createDefaultSettings(UUID clinicId) {
    ClinicNotificationSettings settings = new ClinicNotificationSettings();
    settings.setClinicId(clinicId);
    settings.setWhatsappEnabled(false);
    settings.setSmsFallbackEnabled(true);
    settings.setDefaultPreference(NotificationPreference.SMS_ONLY);
    settings.setWahaSessionName(defaultSessionName(clinicId));
    return settings;
  }

  private String defaultSessionName(UUID clinicId) {
    return "clinic_" + clinicId;
  }
}
