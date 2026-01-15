package ro.cabinet.backend.appointments;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AppointmentService {
  private final AppointmentRepository repository;

  public AppointmentService(AppointmentRepository repository) {
    this.repository = repository;
  }

  public List<Appointment> findByDateRange(Long doctorId, LocalDateTime start, LocalDateTime end) {
    return repository.findByDoctorIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(doctorId, start, end);
  }

  public Appointment create(Appointment appointment) {
    validateAppointment(appointment);
    if (repository.existsByDoctorIdAndStartTimeLessThanAndEndTimeGreaterThan(
        appointment.getDoctorId(), appointment.getEndTime(), appointment.getStartTime())) {
      throw new OverlapException("Exista o programare suprapusa.");
    }
    return repository.save(appointment);
  }

  public Appointment update(Long id, Appointment appointment) {
    validateAppointment(appointment);
    appointment.setId(id);
    if (appointment.getPatientId() == null) {
      Appointment existing = repository.findById(id)
          .orElseThrow(() -> new NotFoundException("Programare inexistenta."));
      appointment.setPatientId(existing.getPatientId());
    }
    if (repository.existsOverlapExcludingId(
        appointment.getDoctorId(), appointment.getStartTime(), appointment.getEndTime(), id)) {
      throw new OverlapException("Exista o programare suprapusa.");
    }
    return repository.save(appointment);
  }

  public Appointment get(Long id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Programare inexistenta."));
  }

  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new NotFoundException("Programare inexistenta.");
    }
    repository.deleteById(id);
  }

  private void validateAppointment(Appointment appointment) {
    if (appointment.getDoctorId() == null || appointment.getDoctorId() <= 0) {
      throw new ValidationException("Doctor invalid.");
    }
    if (appointment.getPatientName() == null || appointment.getPatientName().isBlank()) {
      throw new ValidationException("Pacient invalid.");
    }
    if (appointment.getStartTime() == null || appointment.getEndTime() == null) {
      throw new ValidationException("Interval invalid.");
    }
    if (!appointment.getEndTime().isAfter(appointment.getStartTime())) {
      throw new ValidationException("End time trebuie sa fie dupa start time.");
    }
  }
}
