import Foundation

// Cross-platform JSON wire format shared between LocalLobbyService and
// RemoteLobbyService. The shape mirrors the Kotlin sealed-class layout
// (`type` discriminator + flat fields) so iOS and Android can interoperate
// over either transport.

enum WireMessageType: String, Codable {
    case lobbyState
    case gameState
    case playerAction
    case startGame
}

enum WireActionType: String, Codable {
    case initialPeek
    case startNewGameRound
    case setReadyForNewGame
    case attemptMatchDiscard
    case draw
    case replace
    case discardForEffect
    case resolveSpecial
    case callCabo
}

enum WireSpecialActionType: String, Codable {
    case none
    case lookOwnCard
    case lookOtherCard
    case swapCards
}

struct WireNetworkMessage: Codable {
    let type: WireMessageType
    let players: [String]?
    let hostName: String?
    let state: WireGameState?
    let action: WirePlayerNetworkAction?

    static func lobbyState(players: [String], hostName: String) -> WireNetworkMessage {
        WireNetworkMessage(type: .lobbyState, players: players, hostName: hostName, state: nil, action: nil)
    }

    static func gameState(_ state: WireGameState) -> WireNetworkMessage {
        WireNetworkMessage(type: .gameState, players: nil, hostName: nil, state: state, action: nil)
    }

    static func playerAction(_ action: WirePlayerNetworkAction) -> WireNetworkMessage {
        WireNetworkMessage(type: .playerAction, players: nil, hostName: nil, state: nil, action: action)
    }

    static func startGame() -> WireNetworkMessage {
        WireNetworkMessage(type: .startGame, players: nil, hostName: nil, state: nil, action: nil)
    }
}

struct WirePlayerNetworkAction: Codable {
    let type: WireActionType
    let playerID: String?
    let index: Int?
    let isReady: Bool?
    let source: WireDrawSource?
    let action: WireSpecialAction?
}

struct WireSpecialAction: Codable {
    let type: WireSpecialActionType
    let playerID: String?
    let cardIndex: Int?
    let targetPlayerID: String?
    let fromPlayerID: String?
    let fromCardIndex: Int?
    let toPlayerID: String?
    let toCardIndex: Int?
}

struct WireGameState: Codable {
    let players: [WirePlayer]
    let deck: [WireCard]
    let discardPile: [WireCard]
    let currentPlayerIndex: Int
    let phase: WireTurnPhase
    let pendingDraw: WirePendingDraw?
    let winnerID: String?
    let caboCallerID: String?
    let rematchRequestedByHost: Bool
    let readyPlayerIDs: [String]
    let hasStarted: Bool
    let initialPeekedIndicesByPlayerIndex: [[Int]]
    let playersFinishedInitialPeek: Int
    let initialPeekGraceEndsAt: Int64?
}

struct WirePlayer: Codable {
    let id: String
    let name: String
    let hand: [WireCard?]
    let roundsToSkip: Int
}

struct WirePendingDraw: Codable {
    let card: WireCard
    let source: WireDrawSource
}

struct WireCard: Codable {
    let id: String
    let suit: WireSuit
    let rank: WireRank
}

enum WireSuit: String, Codable {
    case hearts = "HEARTS"
    case diamonds = "DIAMONDS"
    case clubs = "CLUBS"
    case spades = "SPADES"
}

enum WireRank: String, Codable {
    case ace = "ACE"
    case two = "TWO"
    case three = "THREE"
    case four = "FOUR"
    case five = "FIVE"
    case six = "SIX"
    case seven = "SEVEN"
    case eight = "EIGHT"
    case nine = "NINE"
    case ten = "TEN"
    case jack = "JACK"
    case queen = "QUEEN"
    case king = "KING"
}

enum WireDrawSource: String, Codable {
    case deck = "DECK"
    case discardTop = "DISCARD_TOP"
}

enum WireTurnPhase: String, Codable {
    case initialPeek = "INITIAL_PEEK"
    case waitingForDraw = "WAITING_FOR_DRAW"
    case waitingForPlacementOrDiscard = "WAITING_FOR_PLACEMENT_OR_DISCARD"
    case waitingForSpecialResolution = "WAITING_FOR_SPECIAL_RESOLUTION"
}

extension WireNetworkMessage {
    init(from app: NetworkMessage) {
        switch app {
        case .lobbyState(let players, let hostName):
            self = .lobbyState(players: players, hostName: hostName)
        case .gameState(let state):
            self = .gameState(WireGameState(from: state))
        case .playerAction(let action):
            self = .playerAction(WirePlayerNetworkAction(from: action))
        case .startGame:
            self = .startGame()
        }
    }

    func toAppMessage() -> NetworkMessage? {
        switch type {
        case .lobbyState:
            return .lobbyState(players: players ?? [], hostName: hostName ?? "")
        case .gameState:
            guard let state else { return nil }
            return .gameState(state.toAppState())
        case .playerAction:
            guard let action, let appAction = action.toAppAction() else { return nil }
            return .playerAction(appAction)
        case .startGame:
            return .startGame
        }
    }
}

extension WirePlayerNetworkAction {
    init(from app: PlayerNetworkAction) {
        switch app {
        case .initialPeek(let playerID, let index):
            self = .init(type: .initialPeek, playerID: playerID.uuidString, index: index, isReady: nil, source: nil, action: nil)
        case .startNewGameRound(let playerID):
            self = .init(type: .startNewGameRound, playerID: playerID.uuidString, index: nil, isReady: nil, source: nil, action: nil)
        case .setReadyForNewGame(let playerID, let isReady):
            self = .init(type: .setReadyForNewGame, playerID: playerID.uuidString, index: nil, isReady: isReady, source: nil, action: nil)
        case .attemptMatchDiscard(let playerID, let index):
            self = .init(type: .attemptMatchDiscard, playerID: playerID.uuidString, index: index, isReady: nil, source: nil, action: nil)
        case .draw(let playerID, let source):
            self = .init(type: .draw, playerID: playerID.uuidString, index: nil, isReady: nil, source: WireDrawSource(from: source), action: nil)
        case .replace(let playerID, let index):
            self = .init(type: .replace, playerID: playerID.uuidString, index: index, isReady: nil, source: nil, action: nil)
        case .discardForEffect(let playerID):
            self = .init(type: .discardForEffect, playerID: playerID.uuidString, index: nil, isReady: nil, source: nil, action: nil)
        case .resolveSpecial(let playerID, let action):
            self = .init(type: .resolveSpecial, playerID: playerID.uuidString, index: nil, isReady: nil, source: nil, action: WireSpecialAction(from: action))
        case .callCabo(let playerID):
            self = .init(type: .callCabo, playerID: playerID.uuidString, index: nil, isReady: nil, source: nil, action: nil)
        }
    }

    func toAppAction() -> PlayerNetworkAction? {
        switch type {
        case .initialPeek:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)), let idx = index else { return nil }
            return .initialPeek(playerID: pid, index: idx)
        case .startNewGameRound:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)) else { return nil }
            return .startNewGameRound(playerID: pid)
        case .setReadyForNewGame:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)), let isReady else { return nil }
            return .setReadyForNewGame(playerID: pid, isReady: isReady)
        case .attemptMatchDiscard:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)), let idx = index else { return nil }
            return .attemptMatchDiscard(playerID: pid, index: idx)
        case .draw:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)), let source else { return nil }
            return .draw(playerID: pid, source: source.toApp())
        case .replace:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)), let idx = index else { return nil }
            return .replace(playerID: pid, index: idx)
        case .discardForEffect:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)) else { return nil }
            return .discardForEffect(playerID: pid)
        case .resolveSpecial:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)),
                  let action,
                  let appAction = action.toApp()
            else { return nil }
            return .resolveSpecial(playerID: pid, action: appAction)
        case .callCabo:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)) else { return nil }
            return .callCabo(playerID: pid)
        }
    }
}

extension WireSpecialAction {
    init(from app: SpecialAction) {
        switch app {
        case .none:
            self = .init(type: .none, playerID: nil, cardIndex: nil, targetPlayerID: nil, fromPlayerID: nil, fromCardIndex: nil, toPlayerID: nil, toCardIndex: nil)
        case .lookOwnCard(let playerID, let cardIndex):
            self = .init(type: .lookOwnCard, playerID: playerID.uuidString, cardIndex: cardIndex, targetPlayerID: nil, fromPlayerID: nil, fromCardIndex: nil, toPlayerID: nil, toCardIndex: nil)
        case .lookOtherCard(let targetPlayerID, let cardIndex):
            self = .init(type: .lookOtherCard, playerID: nil, cardIndex: cardIndex, targetPlayerID: targetPlayerID.uuidString, fromPlayerID: nil, fromCardIndex: nil, toPlayerID: nil, toCardIndex: nil)
        case .swapCards(let fromPlayerID, let fromCardIndex, let toPlayerID, let toCardIndex):
            self = .init(type: .swapCards, playerID: nil, cardIndex: nil, targetPlayerID: nil, fromPlayerID: fromPlayerID.uuidString, fromCardIndex: fromCardIndex, toPlayerID: toPlayerID.uuidString, toCardIndex: toCardIndex)
        }
    }

    func toApp() -> SpecialAction? {
        switch type {
        case .none:
            return .none
        case .lookOwnCard:
            guard let pid = playerID.flatMap(UUID.init(uuidString:)), let idx = cardIndex else { return nil }
            return .lookOwnCard(playerID: pid, cardIndex: idx)
        case .lookOtherCard:
            guard let pid = targetPlayerID.flatMap(UUID.init(uuidString:)), let idx = cardIndex else { return nil }
            return .lookOtherCard(targetPlayerID: pid, cardIndex: idx)
        case .swapCards:
            guard let fromPID = fromPlayerID.flatMap(UUID.init(uuidString:)),
                  let fromIdx = fromCardIndex,
                  let toPID = toPlayerID.flatMap(UUID.init(uuidString:)),
                  let toIdx = toCardIndex
            else { return nil }
            return .swapCards(fromPlayerID: fromPID, fromCardIndex: fromIdx, toPlayerID: toPID, toCardIndex: toIdx)
        }
    }
}

extension WireGameState {
    init(from app: GameState) {
        self.players = app.players.map(WirePlayer.init(from:))
        self.deck = app.deck.map(WireCard.init(from:))
        self.discardPile = app.discardPile.map(WireCard.init(from:))
        self.currentPlayerIndex = app.currentPlayerIndex
        self.phase = WireTurnPhase(from: app.phase)
        self.pendingDraw = app.pendingDraw.map(WirePendingDraw.init(from:))
        self.winnerID = app.winnerID?.uuidString
        self.caboCallerID = app.caboCallerID?.uuidString
        self.rematchRequestedByHost = app.rematchRequestedByHost
        self.readyPlayerIDs = app.readyPlayerIDs.map(\.uuidString)
        self.hasStarted = app.hasStarted
        self.initialPeekedIndicesByPlayerIndex = app.initialPeekedIndicesByPlayerIndex
        self.playersFinishedInitialPeek = app.playersFinishedInitialPeek
        self.initialPeekGraceEndsAt = app.initialPeekGraceEndsAt.map { Int64($0.timeIntervalSince1970 * 1000) }
    }

    func toAppState() -> GameState {
        var state = GameState(players: players.map { $0.toApp() })
        state.deck = deck.map { $0.toApp() }
        state.discardPile = discardPile.map { $0.toApp() }
        state.currentPlayerIndex = currentPlayerIndex
        state.phase = phase.toApp()
        state.pendingDraw = pendingDraw.map { $0.toApp() }
        state.winnerID = winnerID.flatMap(UUID.init(uuidString:))
        state.caboCallerID = caboCallerID.flatMap(UUID.init(uuidString:))
        state.rematchRequestedByHost = rematchRequestedByHost
        state.readyPlayerIDs = Set(readyPlayerIDs.compactMap(UUID.init(uuidString:)))
        state.hasStarted = hasStarted
        state.initialPeekedIndicesByPlayerIndex = initialPeekedIndicesByPlayerIndex
        state.playersFinishedInitialPeek = playersFinishedInitialPeek
        state.initialPeekGraceEndsAt = initialPeekGraceEndsAt.map { Date(timeIntervalSince1970: TimeInterval($0) / 1000) }
        return state
    }
}

extension WirePlayer {
    init(from app: Player) {
        self.id = app.id.uuidString
        self.name = app.name
        self.hand = app.hand.map { $0.map(WireCard.init(from:)) }
        self.roundsToSkip = app.roundsToSkip
    }

    func toApp() -> Player {
        Player(
            id: UUID(uuidString: id) ?? UUID(),
            name: name,
            hand: hand.map { $0?.toApp() },
            roundsToSkip: roundsToSkip
        )
    }
}

extension WirePendingDraw {
    init(from app: PendingDraw) {
        self.card = WireCard(from: app.card)
        self.source = WireDrawSource(from: app.source)
    }

    func toApp() -> PendingDraw {
        PendingDraw(card: card.toApp(), source: source.toApp())
    }
}

extension WireCard {
    init(from app: Card) {
        self.id = app.id.uuidString
        self.suit = WireSuit(from: app.suit)
        self.rank = WireRank(from: app.rank)
    }

    func toApp() -> Card {
        Card(
            id: UUID(uuidString: id) ?? UUID(),
            suit: suit.toApp(),
            rank: rank.toApp()
        )
    }
}

extension WireSuit {
    init(from app: Suit) {
        switch app {
        case .hearts: self = .hearts
        case .diamonds: self = .diamonds
        case .clubs: self = .clubs
        case .spades: self = .spades
        }
    }

    func toApp() -> Suit {
        switch self {
        case .hearts: return .hearts
        case .diamonds: return .diamonds
        case .clubs: return .clubs
        case .spades: return .spades
        }
    }
}

extension WireRank {
    init(from app: Rank) {
        switch app {
        case .ace: self = .ace
        case .two: self = .two
        case .three: self = .three
        case .four: self = .four
        case .five: self = .five
        case .six: self = .six
        case .seven: self = .seven
        case .eight: self = .eight
        case .nine: self = .nine
        case .ten: self = .ten
        case .jack: self = .jack
        case .queen: self = .queen
        case .king: self = .king
        }
    }

    func toApp() -> Rank {
        switch self {
        case .ace: return .ace
        case .two: return .two
        case .three: return .three
        case .four: return .four
        case .five: return .five
        case .six: return .six
        case .seven: return .seven
        case .eight: return .eight
        case .nine: return .nine
        case .ten: return .ten
        case .jack: return .jack
        case .queen: return .queen
        case .king: return .king
        }
    }
}

extension WireDrawSource {
    init(from app: DrawSource) {
        switch app {
        case .deck: self = .deck
        case .discardTop: self = .discardTop
        }
    }

    func toApp() -> DrawSource {
        switch self {
        case .deck: return .deck
        case .discardTop: return .discardTop
        }
    }
}

extension WireTurnPhase {
    init(from app: TurnPhase) {
        switch app {
        case .initialPeek: self = .initialPeek
        case .waitingForDraw: self = .waitingForDraw
        case .waitingForPlacementOrDiscard: self = .waitingForPlacementOrDiscard
        case .waitingForSpecialResolution: self = .waitingForSpecialResolution
        }
    }

    func toApp() -> TurnPhase {
        switch self {
        case .initialPeek: return .initialPeek
        case .waitingForDraw: return .waitingForDraw
        case .waitingForPlacementOrDiscard: return .waitingForPlacementOrDiscard
        case .waitingForSpecialResolution: return .waitingForSpecialResolution
        }
    }
}
