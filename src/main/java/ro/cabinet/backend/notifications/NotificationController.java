package ro.cabinet.backend.notifications;

import ro.cabinet.backend.notifications.dto.ReminderDispatchRequest;
import ro.cabinet.backend.notifications.dto.ReminderDispatchResponse;
import ro.cabinet.backend.v2.service.ClinicAccessService;
import ro.cabinet.backend.v2.service.CurrentUserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/notifications/reminders")
public class NotificationController {
  private final NotificationService notificationService;
  private final ClinicAccessService clinicAccessService;
  private final CurrentUserService currentUserService;

  public NotificationController(NotificationService notificationService,
                                ClinicAccessService clinicAccessService,
                                CurrentUserService currentUserService) {
    this.notificationService = notificationService;
    this.clinicAccessService = clinicAccessService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/send")
  public ReminderDispatchResponse sendReminder(@Valid @RequestBody ReminderDispatchRequest request) {
    clinicAccessService.requireClinicMembership(request.getClinicId(), currentUserService.requireCurrentUserId());
    return notificationService.dispatchReminder(request);
  }

  @GetMapping("/by-appointment/{appointmentExternalId}")
  public List<ReminderDispatchResponse> byAppointment(@PathVariable String appointmentExternalId) {
    UUID userId = currentUserService.requireCurrentUserId();
    Set<UUID> clinicIds = new HashSet<>();
    notificationService.findOutboxByAppointmentExternalId(appointmentExternalId)
        .forEach(outbox -> clinicIds.add(outbox.getClinicId()));
    for (UUID clinicId : clinicIds) {
      clinicAccessService.requireClinicMembership(clinicId, userId);
    }
    return notificationService.getByAppointmentExternalId(appointmentExternalId);
  }
}
