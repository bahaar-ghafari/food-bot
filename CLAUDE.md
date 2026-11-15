# CLAUDE.md

## ⚠️ Data safety — read before ANY deploy or server command

Never run a command that could delete or overwrite data — especially anything touching the production VPS — without first giving an explicit, prominent warning naming exactly what will be deleted/overwritten, and getting the user's confirmation. This applies even when the command's stated purpose looks unrelated to deletion (a "sync" or "deploy" is still a deletion risk if it uses `--delete`, overwrites files, etc.).

**Incident (2026-08-17):** an `rsync -az --delete` deploy wiped `/opt/foodbot/data/` (all production food data, unrecoverable) because the local repo has no `data/` directory and `--delete` treated the server's live data as "extraneous." No warning was given before running it.

**Rules for this repo specifically:**
- Production data lives ONLY on the server at `/opt/foodbot/data/` (`foods.json`, `languages.json`), bind-mounted into the container. It is never present in the local repo or git.
- Any deploy command touching `/opt/foodbot` must explicitly `--exclude='data/'` (and the stray gitignored root-level `foods.json`/`languages.json` left over from before the app switched to `./data/foods.json`). Never sync that path with a deletion flag without excluding `data/` first.
- Before running `rsync --delete`, `docker volume rm`, `rm -rf`, `git push --force`, or anything else with delete/overwrite semantics against the VPS, stop and confirm with the user first — what will be affected, why, and get an explicit go-ahead.

## 💸 Zero cost — read before adding ANY dependency, service or API

Everything in this project must be free to run. Do not build on anything that costs money: paid APIs (including the Anthropic/Claude API, OpenAI, Google Cloud Vision, etc.), paid tiers, usage-billed cloud services, or anything that needs a credit card or prepaid credits.

- Prefer what already exists (Telegram Bot API, the VPS we already pay for) and free, keyless, or self-hosted options (e.g. open-source libraries running in the container, free public APIs like TheMealDB).
- If a feature genuinely can't be done for free, **say so before implementing it** — name the cost, give the free alternatives (even if worse) — and wait for the user's decision. Never discover the cost after the code is written.
- A free tier that silently turns into billing past a quota counts as paid unless the user explicitly accepts it.

**Incident (2026-11-15):** recipe import from pasted text/screenshots was built on the paid Claude API without mentioning the cost up front.

## Local `.env` (gitignored) — use these, don't ask for them again

Variable names only; never print or commit the values.

- `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME` (no `@`, e.g. `Dishcover_bot` — drives `t.me/<bot>?start=...` share links), `SUPERADMIN_CHAT_ID` — read by the bot.
- `VPS_ROOT_HOST`, `VPS_ROOT_USER`, `VPS_ROOT_PASSWORD` — production server access. For one-off server commands, load the password into `SSHPASS` without echoing it and use `sshpass -e ssh "$VPS_ROOT_USER@$VPS_ROOT_HOST" '...'`. The data-safety rules above still apply: confirm before anything with delete/overwrite semantics.
- Production has its own `/opt/foodbot/.env`; changing a value there needs `docker compose up -d` (not `restart`) to take effect.

## Deployment

**Deploys are automatic.** Every push to `main` triggers `.github/workflows/deploy.yml`: it runs `mvn test`, then SSHes to the VPS and runs `git fetch && git reset --hard origin/main && docker compose up -d --build` in `/opt/foodbot`. So committing + pushing to `main` *is* the deploy — don't run rsync or any manual deploy afterward, and don't offer to. The server uses git (not rsync), which only touches tracked files, so `data/`, `backups/` and `.env` on the VPS are not affected. Failing tests block the deploy. Each deploy also writes `/opt/foodbot/deploy-info.env` (`DEPLOY_COMMIT`, `DEPLOYED_AT`, gitignored, loaded by compose) so the superadmin can check the live version under Settings → "🚀 Deploy info".

See README.md "Production deployment (Docker)" section for the full deploy process.
