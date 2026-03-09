package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.service.CurrentUserService;
import ro.cabinet.backend.v2.service.LegacyAppointmentCopyService;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/appointments/legacy-copy")
public class LegacyAppointmentCopyController {
  private final LegacyAppointmentCopyService legacyAppointmentCopyService;
  private final CurrentUserService currentUserService;

  public LegacyAppointmentCopyController(LegacyAppointmentCopyService legacyAppointmentCopyService,
                                         CurrentUserService currentUserService) {
    this.legacyAppointmentCopyService = legacyAppointmentCopyService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/status")
  public CopyStatusResponse status(@RequestParam("clinicId") UUID clinicId,
                                   @RequestParam("doctorId") UUID doctorId) {
    LegacyAppointmentCopyService.CopyStatus status = legacyAppointmentCopyService.status(
        clinicId,
        doctorId,
        currentUserService.requireCurrentUserId());
    return new CopyStatusResponse(
        status.legacyTotal(),
        status.copiedCount(),
        status.pendingCount(),
        status.alreadyCopied(),
        status.lastCopiedAt());
  }

  @PostMapping
  public CopyResultResponse copy(@Valid @RequestBody CopyRequest request) {
    LegacyAppointmentCopyService.CopyResult result = legacyAppointmentCopyService.copy(
        request.clinicId(),
        request.doctorId(),
        currentUserService.requireCurrentUserId());
    return new CopyResultResponse(
        result.scannedCount(),
        result.copiedNow(),
        result.skippedAlreadyCopied(),
        result.legacyTotal(),
        result.copiedCount(),
        result.pendingCount(),
        result.alreadyCopied(),
        result.lastCopiedAt());
  }

  public record CopyRequest(@NotNull UUID clinicId, @NotNull UUID doctorId) {
  }

  public record CopyStatusResponse(long legacyTotal,
                                   long copiedCount,
                                   long pendingCount,
                                   boolean alreadyCopied,
                                   OffsetDateTime lastCopiedAt) {
  }

  public record CopyResultResponse(int scannedCount,
                                   int copiedNow,
                                   int skippedAlreadyCopied,
                                   long legacyTotal,
                                   long copiedCount,
                                   long pendingCount,
                                   boolean alreadyCopied,
                                   OffsetDateTime lastCopiedAt) {
  }
}
