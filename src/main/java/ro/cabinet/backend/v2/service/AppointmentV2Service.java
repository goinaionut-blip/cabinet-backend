package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.entity.AppointmentV2;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.AppointmentV2Repository;
import ro.cabinet.backend.v2.repo.DoctorRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentV2Service {
  private final AppointmentV2Repository appointmentRepository;
  private final ClinicAccessService clinicAccessService;
  private final DoctorRepository doctorRepository;

  public AppointmentV2Service(AppointmentV2Repository appointmentRepository,
                              ClinicAccessService clinicAccessService,
                              DoctorRepository doctorRepository) {
    this.appointmentRepository = appointmentRepository;
    this.clinicAccessService = clinicAccessService;
    this.doctorRepository = doctorRepository;
  }

  @Transactional(readOnly = true)
  public List<AppointmentV2> list(UUID clinicId, UUID doctorId, OffsetDateTime start, OffsetDateTime end,
                                  UUID userId) {
    clinicAccessService.requireClinicMembership(clinicId, userId);
    validateTimeRange(start, end);
    if (doctorId == null) {
      return appointmentRepository.findByClinicIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
          clinicId, start, end);
    }
    return appointmentRepository
        .findByClinicIdAndDoctorIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
            clinicId, doctorId, start, end);
  }

  @Transactional(readOnly = true)
  public AppointmentV2 get(UUID id, UUID userId) {
    AppointmentV2 appointment = appointmentRepository.findById(id)
        .orElseThrow(() -> new V2NotFoundException("Appointment not found"));
    clinicAccessService.requireClinicMembership(appointment.getClinicId(), userId);
    return appointment;
  }

  @Transactional
  public AppointmentV2 create(AppointmentV2 request, UUID userId) {
    clinicAccessService.requireClinicMembership(request.getClinicId(), userId);
    validateAppointment(request);
    if (!doctorRepository.existsByIdAndClinicIdAndActiveTrue(request.getDoctorId(), request.getClinicId())) {
      throw new V2ValidationException("Doctor not found in clinic");
    }

    request.setId(UUID.randomUUID());
    if (request.getStatus() == null || request.getStatus().isBlank()) {
      request.setStatus("SCHEDULED");
    }
    return appointmentRepository.save(request);
  }

  @Transactional
  public AppointmentV2 update(UUID id, AppointmentV2 request, UUID userId) {
    AppointmentV2 existing = appointmentRepository.findById(id)
        .orElseThrow(() -> new V2NotFoundException("Appointment not found"));
    clinicAccessService.requireClinicMembership(existing.getClinicId(), userId);

    request.setId(id);
    request.setClinicId(existing.getClinicId());
    validateAppointment(request);
    if (!doctorRepository.existsByIdAndClinicIdAndActiveTrue(request.getDoctorId(), existing.getClinicId())) {
      throw new V2ValidationException("Doctor not found in clinic");
    }
    if (request.getStatus() == null || request.getStatus().isBlank()) {
      request.setStatus(existing.getStatus());
    }
    return appointmentRepository.save(request);
  }

  @Transactional
  public void delete(UUID id, UUID userId) {
    AppointmentV2 existing = appointmentRepository.findById(id)
        .orElseThrow(() -> new V2NotFoundException("Appointment not found"));
    clinicAccessService.requireClinicMembership(existing.getClinicId(), userId);
    appointmentRepository.delete(existing);
  }

  private void validateAppointment(AppointmentV2 appointment) {
    if (appointment.getClinicId() == null) {
      throw new V2ValidationException("clinicId is required");
    }
    if (appointment.getDoctorId() == null) {
      throw new V2ValidationException("doctorId is required");
    }
    if (appointment.getPatientName() == null || appointment.getPatientName().isBlank()) {
      throw new V2ValidationException("patientName is required");
    }
    validateTimeRange(appointment.getStartTime(), appointment.getEndTime());
  }

  private void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
    if (start == null || end == null) {
      throw new V2ValidationException("start and end are required");
    }
    if (!end.isAfter(start)) {
      throw new V2ValidationException("endTime must be after startTime");
    }
  }
}
