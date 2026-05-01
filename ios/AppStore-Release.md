# App Store release (iOS)

Use this as a checklist before submitting **Cabo** in App Store Connect.

## Already in the project

- **Display name:** Cabo (`INFOPLIST_KEY_CFBundleDisplayName`)
- **Category:** Card games
- **Local network:** `NSLocalNetworkUsageDescription` + Bonjour service `_cabo-local._tcp` for Same Wi‑Fi lobbies
- **Privacy manifest:** `PrivacyInfo.xcprivacy` (User Defaults / `CA92.1` for optional relay URL override)
- **Export compliance:** `ITSAppUsesNonExemptEncryption` = **NO** in the generated Info.plist (standard HTTPS / `wss://` only → simplified U.S. export reporting in App Store Connect)

## You must do in Apple Developer / Xcode

1. **Apple Developer Program** — Enroll if needed; create an **App ID** matching `PRODUCT_BUNDLE_IDENTIFIER` (currently `com.navitech.cabo`). Register this exact ID under the Navitech team in the Developer portal.
2. **Signing** — In Xcode: target **Signing** → Team → **Automatically manage signing**. For Archive, use **Any iOS Device** then **Product → Archive** with **Release** configuration.
3. **App Store Connect** — New app → same bundle ID → fill name, subtitle, keywords, description, support URL, privacy policy URL (required if you collect anything or use networking; recommended for multiplayer).
4. **Screenshots** — 6.7" and 6.1" (or required sizes for your minimum OS); optional iPad if you ship universal (current target is **iPhone only**, `TARGETED_DEVICE_FAMILY = 1`).
5. **Age rating** — Gambling? Usually **None** for a skill/card game; answer the questionnaire honestly.
6. **App Privacy** — Declare networking (relay + optional local discovery). No account system: minimal data. Align answers with `PrivacyInfo.xcprivacy` and real behavior.
7. **Review notes** — Explain: online play uses your WebSocket relay (`ServerConfig.defaultProductionURL`); Same Wi‑Fi uses local network permission. Provide test steps or a demo server if needed.

## Before each upload

- Bump **MARKETING_VERSION** (user-visible version) and **CURRENT_PROJECT_VERSION** (build number) in Xcode or `project.pbxproj`.
- Confirm production relay URL is **wss://** in `ServerConfig.swift`.
- Run a **Release** build on device; smoke-test lobby, online + same Wi‑Fi if possible.

## Legal / brand

- Ensure you have rights to the **Cabo** name and assets in your territories.
- Keep **App Store Connect** app record and **App ID** in sync with `com.navitech.cabo`.
