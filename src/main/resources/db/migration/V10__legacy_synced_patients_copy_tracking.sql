CREATE TABLE IF NOT EXISTS legacy_synced_patient_copies (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
  legacy_patient_id BIGINT NOT NULL REFERENCES synced_patients(patient_id) ON DELETE CASCADE,
  copied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (clinic_id, legacy_patient_id)
);

CREATE INDEX IF NOT EXISTS idx_legacy_synced_patient_copies_clinic
  ON legacy_synced_patient_copies (clinic_id, copied_at DESC);
