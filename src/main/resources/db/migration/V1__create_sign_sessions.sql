CREATE TABLE sign_sessions (
  id UUID PRIMARY KEY,
  token VARCHAR(128) NOT NULL UNIQUE,
  document_id VARCHAR(64) NOT NULL,
  patient_id VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  original_filename TEXT,
  original_content_type VARCHAR(128),
  original_sha256 VARCHAR(64),
  original_path TEXT,
  signed_filename TEXT,
  signed_content_type VARCHAR(128),
  signed_sha256 VARCHAR(64),
  signed_path TEXT,
  downloaded_at TIMESTAMPTZ
);

CREATE INDEX idx_sign_sessions_document_id ON sign_sessions (document_id);
CREATE INDEX idx_sign_sessions_expires_at ON sign_sessions (expires_at);
