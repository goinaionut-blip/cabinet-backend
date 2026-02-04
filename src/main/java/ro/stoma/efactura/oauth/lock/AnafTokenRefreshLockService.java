package ro.stoma.efactura.oauth.lock;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnafTokenRefreshLockService {
  private static final Duration LOCK_TTL = Duration.ofMinutes(2);
  private final AnafTokenRefreshLockRepository repository;

  public AnafTokenRefreshLockService(AnafTokenRefreshLockRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public boolean tryAcquire(String cif) {
    String key = normalizeKey(cif);
    AnafTokenRefreshLock lock = repository.findByCifForUpdate(key)
        .orElseGet(() -> {
          AnafTokenRefreshLock created = new AnafTokenRefreshLock();
          created.setCif(key);
          created.setRefreshInProgress(false);
          created.setRefreshStartedAt(null);
          return repository.save(created);
        });

    if (lock.isRefreshInProgress() && !isExpired(lock.getRefreshStartedAt())) {
      return false;
    }
    lock.setRefreshInProgress(true);
    lock.setRefreshStartedAt(Instant.now());
    repository.save(lock);
    return true;
  }

  @Transactional
  public void release(String cif) {
    String key = normalizeKey(cif);
    repository.findByCifForUpdate(key).ifPresent(lock -> {
      lock.setRefreshInProgress(false);
      lock.setRefreshStartedAt(null);
      repository.save(lock);
    });
  }

  private boolean isExpired(Instant startedAt) {
    if (startedAt == null) {
      return true;
    }
    return startedAt.isBefore(Instant.now().minus(LOCK_TTL));
  }

  private String normalizeKey(String cif) {
    if (cif == null || cif.isBlank()) {
      return "default";
    }
    return cif;
  }
}
