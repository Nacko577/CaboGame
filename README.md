# CaboGame

SwiftUI iOS starter for Cabo with:
- 4-card hands
- Initial peek at 2 cards only
- Draw from deck or top discard
- Jack: look at one of your own cards
- Queen: look at one card of another player
- King: swap one of your cards with another player's card
- Local multiplayer lobby with join code (nearby via MultipeerConnectivity)

## Project Layout

- `ios/CaboGame/` Swift source files for the iOS app
- `ios/CaboGame/Models/` core entities and action definitions
- `ios/CaboGame/Core/` turn and rules engine
- `ios/CaboGame/Networking/` local peer lobby + game message transport
- `ios/CaboGame/ViewModels/` UI state orchestration
- `ios/CaboGame/Views/` SwiftUI screens

## How To Run

1. Open `CaboGame.xcodeproj` in Xcode.
2. Select your team in Signing for the `CaboGame` target.
3. Run on two physical devices on the same local network/Bluetooth range.

## Regenerating Project File

The project is generated via XcodeGen from `project.yml`.

```bash
xcodegen generate
```

## Current State

This is an implementation starter with playable rules flow and local lobby transport.
You can host/join by code, start a game, and progress turns with rule-driven actions.