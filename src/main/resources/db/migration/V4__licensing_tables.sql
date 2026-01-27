CREATE TABLE IF NOT EXISTS licensing_products (
  product_code VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL,
  trial_days INT NOT NULL DEFAULT 14,
  offline_grace_hours INT NOT NULL DEFAULT 72,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS licensing_installations (
  id UUID PRIMARY KEY,
  product_code VARCHAR NOT NULL REFERENCES licensing_products(product_code),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at TIMESTAMPTZ,
  trial_started_at TIMESTAMPTZ NOT NULL,
  status VARCHAR NOT NULL DEFAULT 'TRIAL',
  blocked_reason VARCHAR,
  app_version VARCHAR,
  UNIQUE (product_code, id)
);

CREATE TABLE IF NOT EXISTS licensing_licenses (
  id UUID PRIMARY KEY,
  product_code VARCHAR NOT NULL REFERENCES licensing_products(product_code),
  license_key_hash VARCHAR NOT NULL UNIQUE,
  plan VARCHAR NOT NULL DEFAULT 'STANDARD',
  status VARCHAR NOT NULL DEFAULT 'ACTIVE',
  max_seats INT NOT NULL DEFAULT 1,
  valid_until TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  notes VARCHAR
);

CREATE TABLE IF NOT EXISTS licensing_activations (
  id UUID PRIMARY KEY,
  license_id UUID NOT NULL REFERENCES licensing_licenses(id),
  install_id UUID NOT NULL,
  product_code VARCHAR NOT NULL,
  activated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at TIMESTAMPTZ,
  UNIQUE (license_id, install_id)
);

CREATE INDEX IF NOT EXISTS idx_licensing_activations_product_install
  ON licensing_activations (product_code, install_id);

CREATE INDEX IF NOT EXISTS idx_licensing_activations_license
  ON licensing_activations (license_id);
