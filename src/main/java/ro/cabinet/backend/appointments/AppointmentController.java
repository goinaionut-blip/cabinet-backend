package ro.cabinet.backend.appointments;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
  private final AppointmentService service;

  public AppointmentController(AppointmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<Appointment> list(@RequestParam("doctorId") Long doctorId,
                                @RequestParam("start") LocalDateTime start,
                                @RequestParam("end") LocalDateTime end) {
    return service.findByDateRange(doctorId, start, end);
  }

  @GetMapping("/{id}")
  public Appointment get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public Appointment create(@Valid @RequestBody AppointmentRequest request) {
    Appointment appointment = map(request);
    return service.create(appointment);
  }

  @PutMapping("/{id}")
  public Appointment update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
    Appointment appointment = map(request);
    return service.update(id, appointment);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  private static Appointment map(AppointmentRequest request) {
    Appointment appointment = new Appointment();
    appointment.setDoctorId(request.doctorId());
    if (request.patientId() != null && request.patientId() > 0) {
      appointment.setPatientId(request.patientId());
    }
    appointment.setPatientName(request.patientName());
    appointment.setStartTime(request.startTime());
    appointment.setEndTime(request.endTime());
    appointment.setNote(request.note());
    return appointment;
  }

  public record AppointmentRequest(
      @NotNull Long doctorId,
      Long patientId,
      @NotNull String patientName,
      @NotNull LocalDateTime startTime,
      @NotNull LocalDateTime endTime,
      String note
  ) {
  }
}
