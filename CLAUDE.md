# ClipVault Project
## Spec
Complete requirements: `android_dev_prompt.md`. Source: `./clipvault/`.
## Environment
- Debian 13, headless (no GUI, no Android Studio)
- JDK: OpenJDK 21 | Android SDK: via `android sdk` commands
- Build verification: `cd clipvault && ./gradlew assembleDebug`
## Android CLI (use instead of raw sdkmanager)
- `android sdk install/list/update` — SDK management
- `android info` — environment info
- `android docs search "<keywords>"` — search official Android docs
- Skills in `.skills/` are auto-loaded
## Rules
- Follow android_dev_prompt.md architecture exactly. Do not redesign.
- Every Task must pass `./gradlew assembleDebug` before moving on.
- After each Task, update `clipvault/PROGRESS.md` with status.
- Use `android docs search` when unsure about any API.
- Do NOT declare `kotlin-android` plugin (AGP 9.x built-in).
- Do NOT upgrade to Room 3.0 (use 2.8.4).
- Do NOT ask me questions — search and decide.
