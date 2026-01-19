package ro.cabinet.backend.sign;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SignSessionRepository extends JpaRepository<SignSession, UUID> {
  Optional<SignSession> findByToken(String token);

  java.util.List<SignSession> findByStatus(SignSessionStatus status);

  java.util.List<SignSession> findByExpiresAtBefore(java.time.OffsetDateTime time);

  java.util.List<SignSession> findByUpdatedAtBefore(java.time.OffsetDateTime time);
}
