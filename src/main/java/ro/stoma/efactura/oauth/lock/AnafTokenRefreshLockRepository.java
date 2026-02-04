package ro.stoma.efactura.oauth.lock;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnafTokenRefreshLockRepository extends JpaRepository<AnafTokenRefreshLock, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from AnafTokenRefreshLock l where l.cif = :cif")
  Optional<AnafTokenRefreshLock> findByCifForUpdate(@Param("cif") String cif);
}
