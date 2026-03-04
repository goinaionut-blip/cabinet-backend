CREATE TABLE IF NOT EXISTS clinics (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  slug TEXT UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS clinic_users (
  clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (clinic_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_clinic_users_user_id ON clinic_users (user_id);
CREATE INDEX IF NOT EXISTS idx_clinic_users_clinic_id ON clinic_users (clinic_id);

CREATE TABLE IF NOT EXISTS doctors (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
  display_name TEXT NOT NULL,
  external_code TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_doctors_clinic_id ON doctors (clinic_id);

CREATE TABLE IF NOT EXISTS appointments_v2 (
  id UUID PRIMARY KEY,
  clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
  doctor_id UUID NOT NULL REFERENCES doctors(id),
  patient_id TEXT,
  patient_name TEXT NOT NULL,
  start_time TIMESTAMPTZ NOT NULL,
  end_time TIMESTAMPTZ NOT NULL,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'SCHEDULED'
);

CREATE INDEX IF NOT EXISTS idx_appointments_v2_clinic_doctor_time
  ON appointments_v2 (clinic_id, doctor_id, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_appointments_v2_clinic_start
  ON appointments_v2 (clinic_id, start_time);

CREATE TABLE IF NOT EXISTS synced_patients_v2 (
  clinic_id UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
  patient_id TEXT NOT NULL,
  patient_name TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (clinic_id, patient_id)
);

CREATE INDEX IF NOT EXISTS idx_synced_patients_v2_clinic_name
  ON synced_patients_v2 (clinic_id, patient_name);
