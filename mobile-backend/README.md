# Customer / Mobile API (CSCSMS)

Spring Boot service for the **ChainStore customer mobile app**.

- Default port: **4314**
- Shares MySQL schema with the staff backend
- Separate JWT secret from staff tokens

## Features

- Register / login with email OTP
- Profile + membership QR
- Points history & loyalty config
- Membership tiers
- Active promotions listing

## Run

```bash
# Set environment variables (see .env.example)
./mvnw spring-boot:run
```

Swagger UI (when running): `/swagger-ui.html`
