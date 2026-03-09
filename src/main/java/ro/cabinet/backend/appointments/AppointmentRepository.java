package ro.cabinet.backend.appointments;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  List<Appointment> findAllByOrderByStartTimeAsc();

  List<Appointment> findByDoctorIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
      Long doctorId, LocalDateTime start, LocalDateTime end);

  boolean existsByDoctorIdAndStartTimeLessThanAndEndTimeGreaterThan(
      Long doctorId, LocalDateTime end, LocalDateTime start);

  @Query("""
      select (count(a) > 0) from Appointment a
      where a.doctorId = :doctorId
        and a.startTime < :end
        and a.endTime > :start
        and a.id <> :id
      """)
  boolean existsOverlapExcludingId(@Param("doctorId") Long doctorId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("id") Long id);
}
