package ro.cabinet.backend.notifications.repo;

import ro.cabinet.backend.notifications.entity.NotificationAttempt;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, UUID> {
}
