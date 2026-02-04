CREATE TABLE IF NOT EXISTS efactura_token_refresh_lock (
  cif VARCHAR(32) PRIMARY KEY,
  refresh_in_progress BOOLEAN NOT NULL DEFAULT FALSE,
  refresh_started_at TIMESTAMP WITH TIME ZONE NULL
);
