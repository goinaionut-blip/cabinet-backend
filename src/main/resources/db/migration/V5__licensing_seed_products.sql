INSERT INTO licensing_products (product_code, name, trial_days, offline_grace_hours)
VALUES ('DENTRX', 'DentRx', 14, 72)
ON CONFLICT (product_code) DO NOTHING;
