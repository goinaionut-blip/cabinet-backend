package ro.cabinet.backend.appointments;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/synced-patients")
public class SyncedPatientController {
  private final SyncedPatientService service;

  public SyncedPatientController(SyncedPatientService service) {
    this.service = service;
  }

  @GetMapping
  public List<SyncedPatient> list() {
    return service.findAll();
  }

  @PutMapping("/{id}")
  public SyncedPatient upsert(@PathVariable Long id, @Valid @RequestBody SyncedPatientRequest request) {
    return service.upsert(id, request.patientName());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  public record SyncedPatientRequest(@NotNull String patientName) {
  }
}
