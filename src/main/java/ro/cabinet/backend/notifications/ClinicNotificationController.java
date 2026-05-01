package ro.cabinet.backend.notifications;

import ro.cabinet.backend.notifications.dto.ClinicNotificationSettingsRequest;
import ro.cabinet.backend.notifications.dto.ClinicNotificationSettingsResponse;
import ro.cabinet.backend.notifications.dto.WahaQrResponse;
import ro.cabinet.backend.notifications.dto.WahaSessionResponse;
import ro.cabinet.backend.v2.service.ClinicAccessService;
import ro.cabinet.backend.v2.service.CurrentUserService;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/clinics/{clinicId}")
public class ClinicNotificationController {
  private final ClinicNotificationSettingsService settingsService;
  private final ClinicAccessService clinicAccessService;
  private final CurrentUserService currentUserService;

  public ClinicNotificationController(ClinicNotificationSettingsService settingsService,
                                      ClinicAccessService clinicAccessService,
                                      CurrentUserService currentUserService) {
    this.settingsService = settingsService;
    this.clinicAccessService = clinicAccessService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/notification-settings")
  public ClinicNotificationSettingsResponse getSettings(@PathVariable UUID clinicId) {
    requireMembership(clinicId);
    return settingsService.getSettingsResponse(clinicId);
  }

  @PutMapping("/notification-settings")
  public ClinicNotificationSettingsResponse updateSettings(@PathVariable UUID clinicId,
                                                           @RequestBody ClinicNotificationSettingsRequest request) {
    requireMembership(clinicId);
    if (request == null) {
      request = new ClinicNotificationSettingsRequest();
    }
    request.setClinicId(clinicId);
    return settingsService.updateSettings(request);
  }

  @PostMapping("/waha/session/start")
  public WahaSessionResponse startSession(@PathVariable UUID clinicId) {
    requireMembership(clinicId);
    return settingsService.createOrStartWahaSession(clinicId);
  }

  @GetMapping("/waha/session/status")
  public WahaSessionResponse sessionStatus(@PathVariable UUID clinicId) {
    requireMembership(clinicId);
    return settingsService.getWahaSessionStatus(clinicId);
  }

  @GetMapping("/waha/session/qr")
  public WahaQrResponse sessionQr(@PathVariable UUID clinicId) {
    requireMembership(clinicId);
    return settingsService.getWahaQr(clinicId);
  }

  private void requireMembership(UUID clinicId) {
    clinicAccessService.requireClinicMembership(clinicId, currentUserService.requireCurrentUserId());
  }
}
