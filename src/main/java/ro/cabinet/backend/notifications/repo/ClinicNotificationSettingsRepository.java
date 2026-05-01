package ro.cabinet.backend.notifications.repo;

import ro.cabinet.backend.notifications.entity.ClinicNotificationSettings;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicNotificationSettingsRepository extends JpaRepository<ClinicNotificationSettings, UUID> {
  Optional<ClinicNotificationSettings> findByClinicId(UUID clinicId);
}
