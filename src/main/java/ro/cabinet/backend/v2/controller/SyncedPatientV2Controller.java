package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.entity.SyncedPatientV2;
import ro.cabinet.backend.v2.service.CurrentUserService;
import ro.cabinet.backend.v2.service.SyncedPatientV2Service;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/synced-patients")
public class SyncedPatientV2Controller {
  private final SyncedPatientV2Service syncedPatientV2Service;
  private final CurrentUserService currentUserService;

  public SyncedPatientV2Controller(SyncedPatientV2Service syncedPatientV2Service,
                                   CurrentUserService currentUserService) {
    this.syncedPatientV2Service = syncedPatientV2Service;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<SyncedPatientV2> list(@RequestParam("clinicId") UUID clinicId) {
    return syncedPatientV2Service.list(clinicId, currentUserService.requireCurrentUserId());
  }

  @PutMapping("/{patientId}")
  public SyncedPatientV2 upsert(@PathVariable String patientId,
                                @RequestParam("clinicId") UUID clinicId,
                                @Valid @RequestBody UpsertSyncedPatientRequest request) {
    return syncedPatientV2Service.upsert(clinicId, patientId, request.patientName(),
        currentUserService.requireCurrentUserId());
  }

  @DeleteMapping("/{patientId}")
  public ResponseEntity<Void> delete(@PathVariable String patientId,
                                     @RequestParam("clinicId") UUID clinicId) {
    syncedPatientV2Service.delete(clinicId, patientId, currentUserService.requireCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  public record UpsertSyncedPatientRequest(@NotBlank String patientName) {
  }
}
