import Foundation

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
    var currentPlayerPeekedIndices: [Int]
    var playersFinishedInitialPeek: Int
    var initialPeekGraceEndsAt: Date?

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
        self.currentPlayerPeekedIndices = []
        self.playersFinishedInitialPeek = 0
        self.initialPeekGraceEndsAt = nil
    }

    var currentPlayerID: UUID? {
        guard players.indices.contains(currentPlayerIndex) else { return nil }
        return players[currentPlayerIndex].id
    }
}
