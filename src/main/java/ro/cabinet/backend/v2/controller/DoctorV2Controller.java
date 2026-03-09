package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.entity.Doctor;
import ro.cabinet.backend.v2.service.CurrentUserService;
import ro.cabinet.backend.v2.service.DoctorV2Service;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/clinics/{clinicId}/doctors")
public class DoctorV2Controller {
  private final DoctorV2Service doctorService;
  private final CurrentUserService currentUserService;

  public DoctorV2Controller(DoctorV2Service doctorService, CurrentUserService currentUserService) {
    this.doctorService = doctorService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<Doctor> list(@PathVariable UUID clinicId) {
    return doctorService.list(clinicId, currentUserService.requireCurrentUserId());
  }

  @PostMapping
  public Doctor create(@PathVariable UUID clinicId, @Valid @RequestBody UpsertDoctorRequest request) {
    return doctorService.create(clinicId, currentUserService.requireCurrentUserId(),
        request.displayName(), request.externalCode(), request.userId());
  }

  @PutMapping("/{doctorId}")
  public Doctor update(@PathVariable UUID clinicId,
                       @PathVariable UUID doctorId,
                       @Valid @RequestBody UpsertDoctorRequest request) {
    return doctorService.update(clinicId, doctorId, currentUserService.requireCurrentUserId(),
        request.displayName(), request.externalCode(), request.userId());
  }

  @DeleteMapping("/{doctorId}")
  public ResponseEntity<Void> delete(@PathVariable UUID clinicId, @PathVariable UUID doctorId) {
    doctorService.softDelete(clinicId, doctorId, currentUserService.requireCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  public record UpsertDoctorRequest(@NotBlank String displayName, String externalCode, UUID userId) {
  }
}
