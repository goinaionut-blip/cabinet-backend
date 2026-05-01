package ro.cabinet.backend.notifications.repo;

import ro.cabinet.backend.notifications.NotificationStatus;
import ro.cabinet.backend.notifications.entity.NotificationOutbox;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {
  List<NotificationOutbox> findTop20ByAppointmentExternalIdOrderByCreatedAtDesc(String appointmentExternalId);

  @Query("""
      select n
      from NotificationOutbox n
      where n.appointmentExternalId = :appointmentExternalId
        and n.patientId = :patientId
        and n.messageText = :messageText
        and n.status in :statuses
        and n.createdAt >= :cutoff
      order by n.createdAt desc
      """)
  List<NotificationOutbox> findRecentDuplicates(@Param("appointmentExternalId") String appointmentExternalId,
                                                @Param("patientId") String patientId,
                                                @Param("messageText") String messageText,
                                                @Param("statuses") Collection<NotificationStatus> statuses,
                                                @Param("cutoff") OffsetDateTime cutoff);
}
