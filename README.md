# Stoma Backend (Spring Boot + PostgreSQL)

## Features
- JWT auth (username/password in config)
- CRUD appointments
- Query appointments by date range
- Overlap validation per doctor
- No scheduler/cron on server

## Config
Edit `backend/src/main/resources/application.yml`:

- `app.auth.users`: hardcoded users (username + password)
- `app.jwt.secret`: JWT secret (min 32 caractere)
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

## API

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

Request body:
```json
{
  "doctorId": 1,
  "patientName": "Popescu Ana",
  "startTime": "2025-01-10T10:00:00",
  "endTime": "2025-01-10T11:00:00",
  "note": "Control"
}
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
