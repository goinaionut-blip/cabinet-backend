package ro.stoma.efactura.oauth.lock;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "efactura_token_refresh_lock")
public class AnafTokenRefreshLock {
  @Id
  @Column(name = "cif", nullable = false, length = 32)
  private String cif;

  @Column(name = "refresh_in_progress", nullable = false)
  private boolean refreshInProgress;

  @Column(name = "refresh_started_at")
  private Instant refreshStartedAt;

  public String getCif() {
    return cif;
  }

  public void setCif(String cif) {
    this.cif = cif;
  }

  public boolean isRefreshInProgress() {
    return refreshInProgress;
  }

  public void setRefreshInProgress(boolean refreshInProgress) {
    this.refreshInProgress = refreshInProgress;
  }

  public Instant getRefreshStartedAt() {
    return refreshStartedAt;
  }

  public void setRefreshStartedAt(Instant refreshStartedAt) {
    this.refreshStartedAt = refreshStartedAt;
  }
}
