package ro.cabinet.backend.notifications.repo;

import ro.cabinet.backend.notifications.NotificationChannel;
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

  List<NotificationOutbox> findTop20ByPatientIdAndReminderTypeCodeOrderByCreatedAtDesc(String patientId, String reminderTypeCode);

  List<NotificationOutbox> findTop20ByPhoneE164AndChannelUsedAndStatusInAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
      String phoneE164, NotificationChannel channelUsed,
      Collection<NotificationStatus> statuses, OffsetDateTime cutoff);

  List<NotificationOutbox> findTop20ByCorrelationCodeAndChannelUsedAndStatusInAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
      String correlationCode, NotificationChannel channelUsed,
      Collection<NotificationStatus> statuses, OffsetDateTime cutoff);

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
