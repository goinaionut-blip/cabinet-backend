package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.entity.AppointmentV2;
import ro.cabinet.backend.v2.service.AppointmentV2Service;
import ro.cabinet.backend.v2.service.CurrentUserService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v2/appointments")
public class AppointmentV2Controller {
  private final AppointmentV2Service appointmentService;
  private final CurrentUserService currentUserService;

  public AppointmentV2Controller(AppointmentV2Service appointmentService,
                                 CurrentUserService currentUserService) {
    this.appointmentService = appointmentService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<AppointmentV2> list(@RequestParam("clinicId") UUID clinicId,
                                  @RequestParam(value = "doctorId", required = false) UUID doctorId,
                                  @RequestParam("start") OffsetDateTime start,
                                  @RequestParam("end") OffsetDateTime end) {
    return appointmentService.list(clinicId, doctorId, start, end, currentUserService.requireCurrentUserId());
  }

  @GetMapping("/{id}")
  public AppointmentV2 get(@PathVariable UUID id) {
    return appointmentService.get(id, currentUserService.requireCurrentUserId());
  }

  @PostMapping
  public AppointmentV2 create(@Valid @RequestBody CreateAppointmentRequest request) {
    AppointmentV2 appointment = new AppointmentV2();
    appointment.setClinicId(request.clinicId());
    appointment.setDoctorId(request.doctorId());
    appointment.setPatientId(request.patientId());
    appointment.setPatientName(request.patientName());
    appointment.setStartTime(request.startTime());
    appointment.setEndTime(request.endTime());
    appointment.setNote(request.note());
    appointment.setStatus(request.status());
    return appointmentService.create(appointment, currentUserService.requireCurrentUserId());
  }

  @PutMapping("/{id}")
  public AppointmentV2 update(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentRequest request) {
    AppointmentV2 appointment = new AppointmentV2();
    appointment.setDoctorId(request.doctorId());
    appointment.setPatientId(request.patientId());
    appointment.setPatientName(request.patientName());
    appointment.setStartTime(request.startTime());
    appointment.setEndTime(request.endTime());
    appointment.setNote(request.note());
    appointment.setStatus(request.status());
    return appointmentService.update(id, appointment, currentUserService.requireCurrentUserId());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    appointmentService.delete(id, currentUserService.requireCurrentUserId());
    return ResponseEntity.noContent().build();
  }

  public record CreateAppointmentRequest(
      @NotNull UUID clinicId,
      @NotNull UUID doctorId,
      String patientId,
      @NotBlank String patientName,
      @NotNull OffsetDateTime startTime,
      @NotNull OffsetDateTime endTime,
      String note,
      String status
  ) {
  }

  public record UpdateAppointmentRequest(
      @NotNull UUID doctorId,
      String patientId,
      @NotBlank String patientName,
      @NotNull OffsetDateTime startTime,
      @NotNull OffsetDateTime endTime,
      String note,
      String status
  ) {
  }
}
