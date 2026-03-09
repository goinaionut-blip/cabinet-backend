CREATE TABLE IF NOT EXISTS legacy_appointment_copies (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
  doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
  legacy_appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
  appointment_v2_id UUID NOT NULL REFERENCES appointments_v2(id) ON DELETE CASCADE,
  copied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (clinic_id, doctor_id, legacy_appointment_id),
  UNIQUE (appointment_v2_id)
);

CREATE INDEX IF NOT EXISTS idx_legacy_appointment_copies_clinic_doctor
  ON legacy_appointment_copies (clinic_id, doctor_id, copied_at DESC);
