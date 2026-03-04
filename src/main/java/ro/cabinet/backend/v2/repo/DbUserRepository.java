package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.DbUser;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DbUserRepository extends JpaRepository<DbUser, UUID> {
  Optional<DbUser> findByEmailIgnoreCase(String email);
}
