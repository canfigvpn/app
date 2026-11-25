# GitHub Direct Sync (Cloudflare removed)

The app stores account records and vehicle data as JSON files in this repository via the GitHub Contents API.

## Required setup
1. Create a **fine-grained personal access token** with access to this repository only and **Contents: Read and write**.
2. Do not use or reuse any token that has been shared in a chat. Revoke exposed tokens.
3. Configure a dedicated data repository if you do not want the data in the app source repository. Update `GITHUB_DATA_REPO` in `index.html` accordingly.
4. To make the app work without prompting each user for a token, set `GITHUB_DATA_TOKEN` in the source before publishing the app. **This is insecure: anyone can extract the token from source/APK and use it.** Keep the token narrowly scoped and monitor/revoke it as needed. Otherwise, users will be prompted for their own token and must have write access to the repository.
5. GitHub API limits and network availability apply. Data files are JSON at `userdata/accounts/<sha256-email>.json` and `userdata/data/<sha256-email>.json`.

## Privacy
Files stored in a public repository are public, including the email/account records and vehicle data. Use a private data repository to restrict visibility, but the client token still carries a security risk. Passwords are stored as PBKDF2-SHA256 hashes with per-account salts, not plaintext.

## Cloudflare
Cloudflare Worker and D1 backend files were removed as part of the migration.
