ALTER TABLE sign_sessions
  ADD COLUMN template_id VARCHAR(32);

CREATE INDEX idx_sign_sessions_template_id ON sign_sessions (template_id);
