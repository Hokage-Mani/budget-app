# Ledger — a live-updating personal budget

Spring Boot backend (Java 17) + a plain HTML/JS frontend served out of the same jar.
No build tooling on the frontend — open `index.html` mentally as "app.js talking to the API."

## What's implemented

| Feature you asked for | How it works |
|---|---|
| Running balances | `Account.balance` is updated in `BalanceService` every time a transaction posts, not recalculated by summing history — so reads are instant. |
| Active / paid-off debts | `Debt.currentBalance` drops with each linked payment; flips `paidOff=true` automatically at $0. |
| Recommended allocation ($ and %) | `DebtPayoffRecommendationService` — avalanche / snowball / weighted strategies, driven by the `SavingsGoal.monthlyAllocation` you set. |
| Color-coded chart | `Chart.js` bar chart, green bars = positive (income/paydown), red = negative (expenses), fed by `DashboardSnapshot.chartSeries`. |
| Calendar | Custom lightweight month grid (no dependency) showing daily net flow and debt due dates. |
| **Live updating** | Spring `WebSocket`/STOMP: any transaction triggers `DashboardService.broadcast()`, which pushes a fresh snapshot to `/topic/dashboard/{userId}`. Every open browser tab re-renders instantly — no polling, no refresh. |
| Multiple people using it | JWT auth (`/api/auth/register`, `/api/auth/login`) — every user's accounts/debts/goals are scoped to their own row via `owner`. |

## Project layout

```
src/main/java/com/budgetapp/
  model/        Account, Transaction, Debt, SavingsGoal, User (JPA entities)
  repository/   Spring Data JPA interfaces
  service/      BalanceService, DebtPayoffRecommendationService, DashboardService
  controller/   REST endpoints (Auth, Account, Debt, Transaction, SavingsGoal, Dashboard)
  security/     JWT issuing/validation, auth filter
  config/       SecurityConfig, WebSocketConfig
src/main/resources/
  application.yml        H2 for dev, Postgres for prod (profile-based)
  static/                 index.html, css/style.css, js/app.js — the whole frontend
```

## Run it locally

```bash
mvn spring-boot:run
```

Visit `http://localhost:8080`. It uses a local H2 file database (`./data/budgetdb`) by default —
nothing to install. Register an account in the UI to get started.

## Making it accessible to other people

You have three realistic paths, roughly in order of effort:

### 1. Quickest: a managed platform (recommended to start)
Services like **Railway**, **Render**, or **Fly.io** can build straight from this repo's `Dockerfile`
and give you a public HTTPS URL in a few minutes, with a small free/cheap tier. Steps are basically:
1. Push this project to a GitHub repo.
2. Connect the repo on the platform, point it at the `Dockerfile`.
3. Add a managed Postgres addon (all three offer one) and set these environment variables on
   the app: `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`.
4. Deploy — you get a shareable `https://your-app.up.railway.app`-style URL.

This gets you: HTTPS, a public link to text/email to people, and a real database that survives restarts.

### 2. More control: a small VPS (DigitalOcean, Hetzner, Linode)
```bash
docker build -t ledger .
docker run -d -p 80:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://<host>:5432/ledger \
  -e DATABASE_USERNAME=... -e DATABASE_PASSWORD=... \
  -e JWT_SECRET=$(openssl rand -hex 32) \
  ledger
```
Put **Caddy** or **Nginx** in front for automatic HTTPS on a real domain (Caddy does this with ~3
lines of config). This is more setup but gives you a memorable domain and full control of data.

### 3. If you just want to demo it to yourself/one friend
`mvn spring-boot:run` locally, then use a tunnel like **ngrok** or **Cloudflare Tunnel** to get a
temporary public URL without deploying anywhere. Good for testing, not for something you leave running.

### Before you actually share it with others
- Set a real `JWT_SECRET` (don't use the placeholder in `application.yml`).
- Switch to the `prod` profile so you're on Postgres, not the H2 file (H2 doesn't handle concurrent
  users well and isn't meant for this).
- Tighten `setAllowedOriginPatterns("*")` in `WebSocketConfig` to your actual domain.
- Remove/guard the `/h2-console` route (it's dev-only, already gated behind the `dev` profile's datasource,
  but drop the `permitAll` on it once you deploy).

## Suggestions

- **Recurring transactions**: bills/paychecks that repeat monthly — right now every transaction is
  entered manually; a `RecurringRule` entity + a scheduled job (`@Scheduled`) would auto-post them.
- **Budget categories with monthly caps**: you have `Transaction.category` already — adding a
  monthly limit per category and showing "% of budget used" is a small add-on to `DashboardService`.
- **CSV/bank import**: most banks export CSV/OFX — a one-time importer would save a lot of manual entry.
  This is the single highest-leverage feature if you actually want to use this day-to-day.
- **Payoff projection**: given current extra payment, project the payoff *date* per debt (simple
  amortization math) — pairs nicely with the calendar you already want.
- **Email/push digest**: a weekly summary email (balances, upcoming due dates) using the same
  `DashboardSnapshot` — you already have all the data assembled.
- **Mobile**: the frontend is plain HTML/CSS/JS with no framework lock-in, so wrapping it in
  something like Capacitor later for a mobile app is straightforward if you want that eventually.

## A note on next steps for the code itself

This is a working skeleton, not a finished product — some things intentionally left simple to keep
the architecture readable:
- No password reset flow yet.
- No pagination on transaction history (fine for personal use, would matter at scale).
- Validation is minimal (`spring-boot-starter-validation` is included but not yet wired into DTOs
  with `@Valid`/`@NotNull` annotations — worth adding before real use).
