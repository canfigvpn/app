# Online account + sync backend for Car App

## Setup (Cloudflare free tier)
1. Create a Cloudflare account and install Wrangler (or use Cloudflare Dashboard).
2. Create a D1 database named `car-app-users`.
3. Run the contents of `schema.sql` in that D1 database's Console.
4. Copy the database ID into `wrangler.toml` (replace `REPLACE_WITH_D1_DATABASE_ID`).
5. Deploy the Worker from this folder using Wrangler: `npx wrangler deploy`.
6. Add a Worker secret named `JWT_SECRET` with a long random value: `npx wrangler secret put JWT_SECRET`.
7. Copy the Worker URL (for example `https://car-app-account-sync.<your-subdomain>.workers.dev`).
8. In `index.html`, replace `YOUR-WORKER-NAME.YOUR-SUBDOMAIN.workers.dev` in `ACCOUNT_API_BASE` with the deployed hostname and commit.

## Endpoints
- `POST /api/register` — email, password, name, avatar
- `POST /api/login` — email/password; returns signed 30-day token
- `PUT /api/profile` — profile name/avatar
- `GET /api/data` and `PUT /api/data` — user app data sync

Password hashes use PBKDF2-SHA256 on the Worker. Data is stored per account in D1.
