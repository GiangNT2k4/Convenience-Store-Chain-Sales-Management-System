# Convenience Store Chain Sales Management System (ChainStore)

Capstone platform for a **one-brand, multi-branch SME retail chain** — from HQ operations and in-store POS to customer loyalty on mobile.

- **Live demo (open this):** https://chainstore.site  
- **Staff API:** `https://api.chainstore.site/api` — JWT-protected.  

## Role on this project — Nguyen Truong Giang (Business Analyst)

I analyzed end-to-end business needs and produced the requirements package that drives the product:

- Elicited pain points (Excel-based sales, cash handover, inconsistent promotions/loyalty)
- Defined actors/roles (Admin, Director, BM, WM, Cashier, Inventory Staff, Customer)
- Designed system context & container architecture
- Designed the shared database domain model
- Authored functional specs, user stories, and acceptance criteria across modules
- Clarified POS, shift, campaign, loyalty, refund, and customer-app rules with the delivery team

Implementation artifacts in this repository (web, staff API, customer API) reflect that specification.

## Repository layout

```
frontend/         # React web — HQ consoles + POS
backend/          # Spring Boot staff API (port 4313)
mobile-backend/   # Spring Boot customer / mobile API (port 4314)
docs/             # BA docs + original Report 7 diagrams
docs/source/      # Full Report 7 (.docx)
docs/diagrams/    # Exported figures (context, swimlanes, UC, ERD, sequence)
```

## Documentation

Start here → **[docs/README.md](docs/README.md)**

| Doc | Description |
| --- | --- |
| [Business overview](docs/01-business-overview.md) | Problem, scope, stakeholders |
| [Architecture](docs/02-system-architecture.md) | Context / packages / sample sequence (from Report 7) |
| [Actors & use cases](docs/03-actors-and-roles.md) | UC diagrams and screen flows |
| [Database](docs/04-database-design.md) | ERD from Report 7 |
| [User stories](docs/05-user-stories.md) | Epics + ACs |
| [Functional spec](docs/06-functional-specification.md) | Cross-module business rules |
| [Diagram gallery](docs/07-diagrams-from-report7.md) | Original swimlanes, UC, architecture, DB, sequence |
| [Report 7](docs/source/Report7_FinalProjectReport.docx) | Full Word report |

## Tech stack

| Layer | Stack |
| --- | --- |
| Web / POS UI | React 18, Vite, Tailwind CSS |
| Staff API | Spring Boot 3.5, Java 17, MySQL |
| Customer API | Spring Boot, Java, shared MySQL |
| Payments | Cash + PayOS QR |

## Modules covered by analysis

- Identity & RBAC, branches, catalog  
- Warehouse & branch inventory / supply chain flows  
- Campaign promotions (chain & branch)  
- POS: shift cash, checkout, campaigns, loyalty, refunds  
- Customer mobile: OTP auth, profile, points, promotions  
- Reporting for HQ / branch

## Quick start

### Web frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

### Staff backend

```bash
cd backend
# set DB_*, PAYOS_*, MAIL_* env vars
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```

### Customer / mobile backend

```bash
cd mobile-backend
# set DB_*, CUSTOMER_JWT_SECRET, MAIL_* env vars
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```

> **Security:** real DB / PayOS / mail credentials are **not** committed. Use `.env.example` files as templates.

## Product notes for reviewers

- Scope = **one retail brand**, not multi-tenant SaaS  
- POS discounts = **campaigns** (voucher codes retired)  
- Customer loyalty is **chain-wide**  
- See `docs/06-functional-specification.md` for explicit limitations (offline, split tender, etc.)
