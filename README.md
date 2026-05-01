# CaboGame

iOS + Android Cabo with:
- 4-card hands
- Initial peek at 2 cards only
- Draw from deck or top discard
- Jack: look at one of your own cards
- Queen: look at one card of another player
- King: swap one of your cards with another player's card
- **Online lobbies** via a tiny relay server (play with friends across networks)
- **Same Wi-Fi lobbies** as a fallback (Bonjour / NSD + UDP discovery)

## Project Layout

- `ios/CaboGame/` Swift sources for the iOS app
- `android/app/src/main/kotlin/com/alex/cabogame/` Kotlin sources for the Android app
- `server/` Tiny Node.js + WebSocket relay server (so friends can play across networks)

Inside each app:

- `Models` / `models` — core entities and action definitions
- `Core` / `engine` — turn and rules engine
- `Networking` / `networking` — `LobbyService` interface plus two implementations:
  - `LocalLobbyService` — peer-to-peer over the local network (NSD/Bonjour + TCP)
  - `RemoteLobbyService` — WebSocket transport that talks to the relay server
- `ViewModels` / `viewmodel` — UI state orchestration
- `Views` / `ui` — SwiftUI / Jetpack Compose screens

## Running the Server

The server is the missing piece for cross-network play. Without it, the Online
toggle in the lobby has nothing to talk to.

See [`server/README.md`](server/README.md) for full deploy instructions. Quick
version:

```bash
cd server
npm install
npm start    # listens on :8080
```

For deployment, the recommended free option is **Render** — push the repo to
GitHub, create a Blueprint instance, and let it pick up `server/render.yaml`.
You'll get a URL like `https://cabo-relay.onrender.com`. The WebSocket URL is
that with the scheme swapped: `wss://cabo-relay.onrender.com/ws`.

## Pointing the Apps at Your Server

After deploying, plug the `wss://` URL into both apps:

- **iOS:** edit `defaultProductionURL` in
  [`ios/CaboGame/Networking/ServerConfig.swift`](ios/CaboGame/Networking/ServerConfig.swift).
  You can also override at runtime without rebuilding by setting the user
  default `cabogame.serverURL`:
  ```bash
  xcrun simctl spawn booted defaults write com.navitech.cabo cabogame.serverURL "wss://your-server/ws"
  ```
- **Android:** edit `DEFAULT_PRODUCTION_URL` in
  [`android/app/src/main/kotlin/com/alex/cabogame/networking/ServerConfig.kt`](android/app/src/main/kotlin/com/alex/cabogame/networking/ServerConfig.kt).

The lobby screen has an **Online / Same Wi-Fi** toggle. Online uses the relay
server above; Same Wi-Fi uses the existing peer-to-peer transport.

### Local development (testing the server with a phone)

When pointing a real phone at a server running on your laptop, use the laptop's
LAN IP — `localhost` won't work from the device:

- **iOS:** `ws://192.168.x.x:8080/ws`. iOS App Transport Security blocks
  cleartext `ws://` by default, so for dev builds you'll need to add an
  `NSExceptionDomains` entry for your IP in the generated Info.plist (or set
  `NSAllowsArbitraryLoads`).
- **Android:** same URL; cleartext is fine on emulator/physical for `*:8080`
  because the manifest doesn't restrict cleartext to a specific domain.

For App Store / Play Store builds, host the server with TLS and use `wss://` —
no client-side exceptions needed.

## How To Run

### iOS

```bash
xcodegen generate
open CaboGame.xcodeproj
```

Then select your team in Signing for the `CaboGame` target and run on a
simulator or a real device.

### Android

Open `android/` in Android Studio and run the `app` configuration. You need
Android Studio Hedgehog or newer (Kotlin 2.0 / AGP 8.13).

## Network Architecture

Game logic is **host-authoritative** regardless of transport. The host's
device runs the rules engine and broadcasts `GameState` updates; guests
forward only `PlayerNetworkAction`s. Both transports speak the same JSON wire
format (see `WireFormat.swift` on iOS and the `@Serializable` types in Kotlin),
so an iOS host can play with Android guests and vice versa.

The relay server is a dumb pipe — it doesn't understand the game; it just
routes messages between members of a lobby. That keeps the server tiny,
stateless about gameplay, and free to run on a sleeping/free instance.

## Regenerating Project File (iOS)

The iOS Xcode project is generated via XcodeGen from `project.yml`:

```bash
xcodegen generate
```
