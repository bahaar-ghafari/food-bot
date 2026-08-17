# CLAUDE.md

## ⚠️ Data safety — read before ANY deploy or server command

Never run a command that could delete or overwrite data — especially anything touching the production VPS — without first giving an explicit, prominent warning naming exactly what will be deleted/overwritten, and getting the user's confirmation. This applies even when the command's stated purpose looks unrelated to deletion (a "sync" or "deploy" is still a deletion risk if it uses `--delete`, overwrites files, etc.).

**Incident (2026-08-17):** an `rsync -az --delete` deploy wiped `/opt/foodbot/data/` (all production food data, unrecoverable) because the local repo has no `data/` directory and `--delete` treated the server's live data as "extraneous." No warning was given before running it.

**Rules for this repo specifically:**
- Production data lives ONLY on the server at `/opt/foodbot/data/` (`foods.json`, `languages.json`), bind-mounted into the container. It is never present in the local repo or git.
- Any deploy command touching `/opt/foodbot` must explicitly `--exclude='data/'` (and the stray gitignored root-level `foods.json`/`languages.json` left over from before the app switched to `./data/foods.json`). Never sync that path with a deletion flag without excluding `data/` first.
- Before running `rsync --delete`, `docker volume rm`, `rm -rf`, `git push --force`, or anything else with delete/overwrite semantics against the VPS, stop and confirm with the user first — what will be affected, why, and get an explicit go-ahead.

## Deployment

See README.md "Production deployment (Docker)" section for the full deploy process.
