import Foundation

struct KingSwapHighlight: Codable, Equatable {
    var fromPlayerID: UUID
    var fromHandIndex: Int
    var toPlayerID: UUID
    var toHandIndex: Int
}

struct GameState: Codable {
    var players: [Player]
    var deck: [Card]
    var discardPile: [Card]
    var currentPlayerIndex: Int
    var phase: TurnPhase
    var pendingDraw: PendingDraw?
    var winnerID: UUID?
    var caboCallerID: UUID?
    var rematchRequestedByHost: Bool
    var readyPlayerIDs: Set<UUID>
    var hasStarted: Bool
    // Initial peek tracking per player (each player can peek up to 2 cards during initialPeek).
    var initialPeekedIndicesByPlayerIndex: [[Int]]
    var playersFinishedInitialPeek: Int
    var initialPeekGraceEndsAt: Date?
    /// Wall-clock deadline after which the current player's turn auto-expires.
    /// `nil` outside of main play (initial peek phase, game over, or before
    /// the first turn begins).
    var currentTurnEndsAt: Date?
    /// When non-nil, clients outline these two hand slots after a King swap.
    var kingSwapHighlight: KingSwapHighlight?

    init(players: [Player] = []) {
        self.players = players
        self.deck = .caboDeck()
        self.discardPile = []
        self.currentPlayerIndex = 0
        self.phase = .waitingForDraw
        self.pendingDraw = nil
        self.winnerID = nil
        self.caboCallerID = nil
        self.rematchRequestedByHost = false
        self.readyPlayerIDs = []
        self.hasStarted = false
        self.initialPeekedIndicesByPlayerIndex = Array(repeating: [], count: players.count)
        self.playersFinishedInitialPeek = 0
        self.initialPeekGraceEndsAt = nil
        self.currentTurnEndsAt = nil
        self.kingSwapHighlight = nil
    }

    var currentPlayerID: UUID? {
        guard players.indices.contains(currentPlayerIndex) else { return nil }
        return players[currentPlayerIndex].id
    }
}
