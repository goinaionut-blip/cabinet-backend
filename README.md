# Stoma Backend (Spring Boot + PostgreSQL)

## Features
- JWT auth v1 (username/password in config)
- JWT auth v2 (users in Postgres)
- CRUD appointments v1 + v2
- Multi-cabinet support in `/api/v2/**`
- Flyway migrations

## Config
Edit `backend/src/main/resources/application.yml`:

- `app.auth.users`: users for v1 login (`/api/auth/login`)
- `app.jwt.secret`: JWT secret (min 32 chars)
- `app.jwt.expirationMinutes`: token TTL

Database envs (Railway):
- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Run locally
```bash
cd backend
mvn spring-boot:run
```

## API v1

### Login
`POST /api/auth/login`
```json
{ "username": "doctor", "password": "doctor123" }
```
Response:
```json
{ "token": "jwt..." }
```

### Appointments
All endpoints require `Authorization: Bearer <token>`

- `GET /api/appointments?doctorId=1&start=2025-01-01T00:00:00&end=2025-01-31T23:59:59`
- `GET /api/appointments/{id}`
- `POST /api/appointments`
- `PUT /api/appointments/{id}`
- `DELETE /api/appointments/{id}`

## API v2 (multi-cabinet)

### 1) Dev bootstrap user in DB (optional)
Production should use your own provisioning flow. For local/dev you can insert one user manually:

```sql
INSERT INTO users (id, email, password_hash, display_name, is_active)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  'owner@local.test',
  '$2a$10$w4M6H86l8KTvM4YkE6mMOuRdQyy5FCLf0fDnlyBYYG6iwSeN5e7gW',
  'Owner Local',
  true
);
```

Password for hash above: `parola123`.

### 2) v2 login
```bash
curl -X POST http://localhost:8080/api/v2/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner@local.test","password":"parola123"}'
```

### 3) create clinic
```bash
curl -X POST http://localhost:8080/api/v2/clinics \
  -H 'Authorization: Bearer <TOKEN_V2>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Clinica Demo","slug":"clinica-demo"}'
```

### 4) create doctor
```bash
curl -X POST http://localhost:8080/api/v2/clinics/<CLINIC_ID>/doctors \
  -H 'Authorization: Bearer <TOKEN_V2>' \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"Dr. Ionescu"}'
```

### 5) create appointment v2
```bash
curl -X POST http://localhost:8080/api/v2/appointments \
  -H 'Authorization: Bearer <TOKEN_V2>' \
  -H 'Content-Type: application/json' \
  -d '{
    "clinicId":"<CLINIC_ID>",
    "doctorId":"<DOCTOR_ID>",
    "patientName":"Popescu Ana",
    "startTime":"2026-03-04T10:00:00Z",
    "endTime":"2026-03-04T10:30:00Z",
    "note":"Control"
  }'
```

### 6) list appointments v2
```bash
curl 'http://localhost:8080/api/v2/appointments?clinicId=<CLINIC_ID>&start=2026-03-01T00:00:00Z&end=2026-03-31T23:59:59Z' \
  -H 'Authorization: Bearer <TOKEN_V2>'
```

## Railway deploy
1) Push repo to GitHub.
2) Create new Railway project from GitHub repo.
3) Set root directory to `backend`.
4) Add env vars:
   - `PORT`
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `APP_JWT_SECRET`
5) Add Railway PostgreSQL and wire envs.
