package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.AppointmentV2;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentV2Repository extends JpaRepository<AppointmentV2, UUID> {
  List<AppointmentV2> findByClinicIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
      UUID clinicId, OffsetDateTime start, OffsetDateTime end);

  List<AppointmentV2> findByClinicIdAndDoctorIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
      UUID clinicId, UUID doctorId, OffsetDateTime start, OffsetDateTime end);
}
