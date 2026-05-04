# CaboGame

iOS + Android Cabo with:

- 4-card hands
- **2–6 players** per lobby (host + up to five guests)
- Initial peek at 2 cards only
- Draw from deck or top discard
- Jack: look at one of your own cards  
- Queen: look at one card of another player  
- King: swap one of your cards with another player's card  
- **Online lobbies** (cross-network play)  
- **Same Wi-Fi lobbies** (Bonjour / NSD + UDP discovery)  
- **How to Play** — in-app visual guide on both platforms (rules, scoring strip, table examples)

## Project Layout

- `ios/CaboGame/` Swift sources for the iOS app  
- `android/app/src/main/kotlin/com/navitech/cabo/` Kotlin sources for the Android app  
- `docs/` Static support pages (e.g. privacy links); not bundled in the apps unless you host them separately  

Inside each app:

- `Models` / `models` — core entities and action definitions  
- `Core` / `engine` — turn and rules engine  
- `Networking` / `networking` — `LobbyService` interface plus two implementations:  
  - `LocalLobbyService` — peer-to-peer over the local network (NSD/Bonjour + TCP)  
  - `RemoteLobbyService` — WebSocket transport for online play  
- `ViewModels` / `viewmodel` — UI state orchestration  
- `Views` / `ui` — SwiftUI / Jetpack Compose screens  

## Store releases (version bumps)

Native updates ship through **Google Play** and the **App Store** as new binaries.

- **Android:** `versionCode` (integer, must increase every upload) and `versionName` (user-visible string) in [`android/app/build.gradle.kts`](android/app/build.gradle.kts) under `defaultConfig`.
- **iOS:** `MARKETING_VERSION` (user-visible) and `CURRENT_PROJECT_VERSION` (build number) in [`CaboGame.xcodeproj/project.pbxproj`](CaboGame.xcodeproj/project.pbxproj) for the main target (Debug and Release).

## Network Architecture

Game logic is **host-authoritative** regardless of transport. The host's device runs the rules engine and broadcasts `GameState` updates; guests forward only `PlayerNetworkAction`s. Both transports speak the same JSON wire format (see `WireFormat.swift` on iOS and the `@Serializable` types in Kotlin), so an iOS host can play with Android guests and vice versa.

## Regenerating Project File (iOS)

The iOS Xcode project is generated via XcodeGen from `project.yml`:

```bash
xcodegen generate
```
