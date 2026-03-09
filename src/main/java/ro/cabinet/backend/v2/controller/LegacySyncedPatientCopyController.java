package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.service.CurrentUserService;
import ro.cabinet.backend.v2.service.LegacySyncedPatientCopyService;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/synced-patients/legacy-copy")
public class LegacySyncedPatientCopyController {
  private final LegacySyncedPatientCopyService legacySyncedPatientCopyService;
  private final CurrentUserService currentUserService;

  public LegacySyncedPatientCopyController(LegacySyncedPatientCopyService legacySyncedPatientCopyService,
                                           CurrentUserService currentUserService) {
    this.legacySyncedPatientCopyService = legacySyncedPatientCopyService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/status")
  public CopyStatusResponse status(@RequestParam("clinicId") UUID clinicId) {
    LegacySyncedPatientCopyService.CopyStatus status = legacySyncedPatientCopyService.status(
        clinicId,
        currentUserService.requireCurrentUserId());
    return new CopyStatusResponse(
        status.legacyTotal(),
        status.copiedCount(),
        status.pendingCount(),
        status.alreadyCopied(),
        status.lastCopiedAt());
  }

  @PostMapping
  public CopyResultResponse copy(@RequestParam("clinicId") UUID clinicId) {
    LegacySyncedPatientCopyService.CopyResult result = legacySyncedPatientCopyService.copy(
        clinicId,
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
