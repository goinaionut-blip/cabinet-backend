package ro.cabinet.backend.sign;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "sign_sessions")
public class SignSession {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 128)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private SignSessionStatus status;

  @Column(name = "document_id", nullable = false, length = 64)
  private String documentId;

  @Column(name = "patient_id", length = 64)
  private String patientId;

  @Column(name = "original_filename")
  private String originalFilename;

  @Column(name = "original_content_type", length = 128)
  private String originalContentType;

  @Column(name = "original_sha256", length = 64)
  private String originalSha256;

  @Column(name = "original_path")
  private String originalPath;

  @Column(name = "signed_filename")
  private String signedFilename;

  @Column(name = "signed_content_type", length = 128)
  private String signedContentType;

  @Column(name = "signed_sha256", length = 64)
  private String signedSha256;

  @Column(name = "signed_path")
  private String signedPath;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "downloaded_at")
  private OffsetDateTime downloadedAt;

  @PrePersist
  public void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  public void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public SignSessionStatus getStatus() {
    return status;
  }

  public void setStatus(SignSessionStatus status) {
    this.status = status;
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  public String getOriginalContentType() {
    return originalContentType;
  }

  public void setOriginalContentType(String originalContentType) {
    this.originalContentType = originalContentType;
  }

  public String getOriginalSha256() {
    return originalSha256;
  }

  public void setOriginalSha256(String originalSha256) {
    this.originalSha256 = originalSha256;
  }

  public String getOriginalPath() {
    return originalPath;
  }

  public void setOriginalPath(String originalPath) {
    this.originalPath = originalPath;
  }

  public String getSignedFilename() {
    return signedFilename;
  }

  public void setSignedFilename(String signedFilename) {
    this.signedFilename = signedFilename;
  }

  public String getSignedContentType() {
    return signedContentType;
  }

  public void setSignedContentType(String signedContentType) {
    this.signedContentType = signedContentType;
  }

  public String getSignedSha256() {
    return signedSha256;
  }

  public void setSignedSha256(String signedSha256) {
    this.signedSha256 = signedSha256;
  }

  public String getSignedPath() {
    return signedPath;
  }

  public void setSignedPath(String signedPath) {
    this.signedPath = signedPath;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public OffsetDateTime getDownloadedAt() {
    return downloadedAt;
  }

  public void setDownloadedAt(OffsetDateTime downloadedAt) {
    this.downloadedAt = downloadedAt;
  }
}
