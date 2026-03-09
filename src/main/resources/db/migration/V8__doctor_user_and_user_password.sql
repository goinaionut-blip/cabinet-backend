ALTER TABLE doctors
  ADD COLUMN IF NOT EXISTS user_id UUID;

CREATE INDEX IF NOT EXISTS idx_doctors_user_id ON doctors (user_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_doctors_user_id'
  ) THEN
    ALTER TABLE doctors
      ADD CONSTRAINT fk_doctors_user_id
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
  END IF;
END $$;
