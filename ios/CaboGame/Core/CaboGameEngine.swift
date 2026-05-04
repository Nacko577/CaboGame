import Foundation

enum GameRuleError: LocalizedError {
    case notEnoughPlayers
    case invalidPlayer
    case invalidPhase
    case emptyDeck
    case noDiscardCard
    case invalidCardIndex
    case noPendingDraw
    case invalidSpecialCard
    case caboAlreadyCalled
    case roundFinished

    var errorDescription: String? {
        switch self {
        case .notEnoughPlayers: return "Need at least 2 players."
        case .invalidPlayer: return "It is not this player's turn."
        case .invalidPhase: return "This action is not valid right now."
        case .emptyDeck: return "Deck is empty."
        case .noDiscardCard: return "No card in discard pile."
        case .invalidCardIndex: return "Selected card index is invalid."
        case .noPendingDraw: return "No drawn card is pending."
        case .invalidSpecialCard: return "This card has no special power."
        case .caboAlreadyCalled: return "Cabo has already been called this round."
        case .roundFinished: return "Round already finished."
        }
    }
}

struct CaboGameEngine {
    /// Maximum length of a single player turn after the initial peek phase.
    /// Matches the Android engine's `TURN_TIMEOUT_SECONDS`.
    static let turnTimeoutSeconds: TimeInterval = 20

    private(set) var state: GameState

    init(players: [Player] = []) {
        self.state = GameState(players: players)
    }

    mutating func addPlayer(name: String) -> UUID {
        let newPlayer = Player(name: name)
        state.players.append(newPlayer)
        return newPlayer.id
    }

    mutating func load(state: GameState) {
        self.state = state
    }

    mutating func startGame() throws {
        guard state.players.count >= 2 else { throw GameRuleError.notEnoughPlayers }
        state.deck = .caboDeck()
        state.discardPile = []
        state.currentPlayerIndex = 0
        state.phase = .initialPeek
        state.pendingDraw = nil
        state.winnerID = nil
        state.caboCallerID = nil
        state.rematchRequestedByHost = false
        state.readyPlayerIDs = []
        state.hasStarted = true
        state.playersFinishedInitialPeek = 0
        state.initialPeekGraceEndsAt = Date().addingTimeInterval(15)
        state.currentTurnEndsAt = nil
        state.kingSwapHighlight = nil
        state.initialPeekedIndicesByPlayerIndex = Array(repeating: [], count: state.players.count)

        for idx in state.players.indices {
            state.players[idx].hand = Array(repeating: nil, count: 4)
            state.players[idx].roundsToSkip = 0
        }

        for slot in 0..<4 {
            for idx in state.players.indices {
                let card = try drawFromDeckInternal()
                state.players[idx].hand[slot] = card
            }
        }

        // Seed discard pile so "draw from discard" is available.
        let firstDiscard = try drawFromDeckInternal()
        state.discardPile.append(firstDiscard)
    }

    mutating func peekInitialCard(for playerID: UUID, at handIndex: Int) throws -> Card {
        guard state.phase == .initialPeek else { throw GameRuleError.invalidPhase }

        let playerIndex = try indexOfPlayer(playerID)
        guard state.players[playerIndex].hand.indices.contains(handIndex) else {
            throw GameRuleError.invalidCardIndex
        }
        guard !state.initialPeekedIndicesByPlayerIndex[playerIndex].contains(handIndex) else {
            throw GameRuleError.invalidCardIndex
        }

        state.initialPeekedIndicesByPlayerIndex[playerIndex].append(handIndex)
        guard let card = state.players[playerIndex].hand[handIndex] else { throw GameRuleError.invalidCardIndex }

        // Count completion when a player reaches 2 peeked cards.
        if state.initialPeekedIndicesByPlayerIndex[playerIndex].count == 2 {
            state.playersFinishedInitialPeek += 1
            // Do not start the game early just because everyone peeked.
            // The phase transition is governed solely by `initialPeekGraceEndsAt`.
        }

        return card
    }

    mutating func beginMainTurnsAfterInitialPeekIfReady(now: Date = Date()) {
        guard state.phase == .initialPeek else { return }
        guard let graceEndsAt = state.initialPeekGraceEndsAt else { return }
        guard now >= graceEndsAt else { return }

        state.phase = .waitingForDraw
        state.currentPlayerIndex = 0
        state.initialPeekedIndicesByPlayerIndex = Array(repeating: [], count: state.players.count)
        state.playersFinishedInitialPeek = 0
        state.initialPeekGraceEndsAt = nil
        state.kingSwapHighlight = nil
        startTurnTimer(now: now)
    }

    /// Forces the current player's turn to end because the per-turn timer
    /// expired. Behavior depends on the phase the player got stuck in:
    ///  - `waitingForDraw`            : skip the turn outright.
    ///  - `waitingForPlacementOrDiscard`: discard the pending drawn card without
    ///                                    applying its effect, then advance.
    ///  - `waitingForSpecialResolution`: forfeit the J/Q/K effect (the card is
    ///                                    already on the discard pile), advance.
    /// Returns `true` if the turn was actually ended, `false` if there was
    /// nothing to do (e.g. game over or initial peek phase).
    @discardableResult
    mutating func enforceTurnTimeoutIfNeeded(now: Date = Date()) -> Bool {
        guard state.winnerID == nil else { return false }
        guard state.phase != .initialPeek else { return false }
        guard let deadline = state.currentTurnEndsAt, now >= deadline else { return false }

        if state.phase == .waitingForPlacementOrDiscard, let pending = state.pendingDraw {
            // Active player drew a card but never placed it: discard the
            // drawn card with no effect and end the turn.
            state.discardPile.append(pending.card)
            state.pendingDraw = nil
        } else if state.phase == .waitingForSpecialResolution {
            // The J/Q/K is already on the discard pile from the prior
            // `discardForEffect` step. We just forfeit the effect.
            state.pendingDraw = nil
        }
        endTurn(now: now)
        return true
    }

    private mutating func startTurnTimer(now: Date = Date()) {
        guard state.winnerID == nil else { return }
        guard state.phase != .initialPeek else { return }
        state.currentTurnEndsAt = now.addingTimeInterval(CaboGameEngine.turnTimeoutSeconds)
    }

    mutating func attemptMatchDiscard(playerID: UUID, handIndex: Int) throws -> Bool {
        guard state.winnerID == nil else { throw GameRuleError.roundFinished }
        guard state.phase != .initialPeek else { throw GameRuleError.invalidPhase }
        state.kingSwapHighlight = nil
        guard let topDiscard = state.discardPile.last else { throw GameRuleError.noDiscardCard }

        let playerIndex = try indexOfPlayer(playerID)
        guard state.players[playerIndex].hand.indices.contains(handIndex) else {
            throw GameRuleError.invalidCardIndex
        }

        guard let selected = state.players[playerIndex].hand[handIndex] else {
            throw GameRuleError.invalidCardIndex
        }
        if selected.rank == topDiscard.rank {
            state.players[playerIndex].hand[handIndex] = nil
            state.discardPile.append(selected)
            return true
        }

        // Failed match: end the matcher's *current* turn if it is theirs;
        // otherwise (an off-turn match attempt) make them sit out their next
        // full turn.
        if state.currentPlayerIndex == playerIndex {
            // Discard any pending drawn card (without applying its effect)
            // so deck/discard counts stay consistent before advancing turn.
            if let pending = state.pendingDraw {
                state.discardPile.append(pending.card)
                state.pendingDraw = nil
            }
            endTurn()
        } else {
            state.players[playerIndex].roundsToSkip += 1
        }
        return false
    }

    mutating func drawCard(for playerID: UUID, source: DrawSource) throws -> Card {
        try validateTurn(for: playerID)
        guard state.phase == .waitingForDraw else { throw GameRuleError.invalidPhase }
        state.kingSwapHighlight = nil

        let card: Card
        switch source {
        case .deck:
            card = try drawFromDeckInternal()
        case .discardTop:
            guard let discardTop = state.discardPile.popLast() else {
                throw GameRuleError.noDiscardCard
            }
            card = discardTop
        }

        state.pendingDraw = PendingDraw(card: card, source: source)
        state.phase = .waitingForPlacementOrDiscard
        return card
    }

    mutating func replaceCard(for playerID: UUID, at handIndex: Int) throws {
        try validateTurn(for: playerID)
        guard state.phase == .waitingForPlacementOrDiscard else { throw GameRuleError.invalidPhase }
        guard let pending = state.pendingDraw else { throw GameRuleError.noPendingDraw }
        state.kingSwapHighlight = nil

        let playerIndex = try indexOfPlayer(playerID)
        guard state.players[playerIndex].hand.indices.contains(handIndex) else {
            throw GameRuleError.invalidCardIndex
        }

        let replaced = state.players[playerIndex].hand[handIndex]
        state.players[playerIndex].hand[handIndex] = pending.card
        if let replaced {
            state.discardPile.append(replaced)
        }

        state.pendingDraw = nil
        endTurn()
    }

    mutating func discardDrawnCardAndUseEffect(for playerID: UUID) throws -> Rank {
        try validateTurn(for: playerID)
        guard state.phase == .waitingForPlacementOrDiscard else { throw GameRuleError.invalidPhase }
        guard let pending = state.pendingDraw else { throw GameRuleError.noPendingDraw }

        state.kingSwapHighlight = nil
        state.discardPile.append(pending.card)
        state.pendingDraw = nil

        switch pending.card.rank {
        case .jack, .queen, .king:
            state.phase = .waitingForSpecialResolution
        default:
            endTurn()
        }
        return pending.card.rank
    }

    mutating func resolveSpecialAction(for playerID: UUID, action: SpecialAction) throws {
        try validateTurn(for: playerID)
        guard state.phase == .waitingForSpecialResolution else { throw GameRuleError.invalidPhase }
        state.kingSwapHighlight = nil

        guard let top = state.discardPile.last else { throw GameRuleError.noDiscardCard }
        switch (top.rank, action) {
        case (.jack, .lookOwnCard(let targetPlayerID, let cardIndex)):
            guard targetPlayerID == playerID else { throw GameRuleError.invalidSpecialCard }
            let playerIndex = try indexOfPlayer(playerID)
            guard state.players[playerIndex].hand.indices.contains(cardIndex) else {
                throw GameRuleError.invalidCardIndex
            }
            guard state.players[playerIndex].hand[cardIndex] != nil else {
                throw GameRuleError.invalidCardIndex
            }

        case (.queen, .lookOtherCard(let targetPlayerID, let cardIndex)):
            let targetIdx = try indexOfPlayer(targetPlayerID)
            guard targetPlayerID != playerID else { throw GameRuleError.invalidSpecialCard }
            guard state.players[targetIdx].hand.indices.contains(cardIndex) else {
                throw GameRuleError.invalidCardIndex
            }
            // Queen allows peeking an opponent card this turn.
            // Client can surface this to the active player; persistent memory is not stored for opponents.
            guard state.players[targetIdx].hand[cardIndex] != nil else {
                throw GameRuleError.invalidCardIndex
            }

        case (.king, .swapCards(let fromPlayerID, let fromCardIndex, let toPlayerID, let toCardIndex)):
            let fromIdx = try indexOfPlayer(fromPlayerID)
            let toIdx = try indexOfPlayer(toPlayerID)
            guard fromPlayerID == playerID, toPlayerID != playerID else {
                throw GameRuleError.invalidSpecialCard
            }
            guard state.players[fromIdx].hand.indices.contains(fromCardIndex),
                  state.players[toIdx].hand.indices.contains(toCardIndex) else {
                throw GameRuleError.invalidCardIndex
            }
            guard state.players[fromIdx].hand[fromCardIndex] != nil,
                  state.players[toIdx].hand[toCardIndex] != nil else {
                throw GameRuleError.invalidCardIndex
            }
            let tmp = state.players[fromIdx].hand[fromCardIndex]
            state.players[fromIdx].hand[fromCardIndex] = state.players[toIdx].hand[toCardIndex]
            state.players[toIdx].hand[toCardIndex] = tmp
            state.kingSwapHighlight = KingSwapHighlight(
                fromPlayerID: fromPlayerID,
                fromHandIndex: fromCardIndex,
                toPlayerID: toPlayerID,
                toHandIndex: toCardIndex
            )

        default:
            throw GameRuleError.invalidSpecialCard
        }

        endTurn()
    }

    mutating func callCabo(for playerID: UUID) throws {
        try validateTurn(for: playerID)
        guard state.phase == .waitingForDraw else { throw GameRuleError.invalidPhase }
        guard state.pendingDraw == nil else { throw GameRuleError.invalidPhase }
        guard state.caboCallerID == nil else { throw GameRuleError.caboAlreadyCalled }

        state.caboCallerID = playerID
        state.kingSwapHighlight = nil
        endTurn()
    }

    private mutating func drawFromDeckInternal() throws -> Card {
        if state.deck.isEmpty {
            try reshuffleDiscardIntoDeckIfNeeded()
        }
        guard let card = state.deck.popLast() else {
            throw GameRuleError.emptyDeck
        }
        return card
    }

    private mutating func reshuffleDiscardIntoDeckIfNeeded() throws {
        guard state.deck.isEmpty else { return }
        guard state.discardPile.count > 1 else { throw GameRuleError.emptyDeck }
        let keepTop = state.discardPile.removeLast()
        state.deck = state.discardPile.shuffled()
        state.discardPile = [keepTop]
    }

    private mutating func endTurn(now: Date = Date()) {
        state.phase = .waitingForDraw
        if !state.players.isEmpty {
            var nextIndex = state.currentPlayerIndex
            var safety = state.players.count
            repeat {
                nextIndex = (nextIndex + 1) % state.players.count
                if state.players[nextIndex].roundsToSkip > 0 {
                    state.players[nextIndex].roundsToSkip -= 1
                } else {
                    break
                }
                safety -= 1
            } while safety > 0
            state.currentPlayerIndex = nextIndex

            if let caboCallerID = state.caboCallerID,
               state.players[nextIndex].id == caboCallerID {
                finishRound()
                return
            }
        }
        startTurnTimer(now: now)
    }

    private mutating func finishRound() {
        let sorted = state.players.sorted { $0.score() < $1.score() }
        state.winnerID = sorted.first?.id
        state.caboCallerID = nil
        state.phase = .waitingForDraw
        state.pendingDraw = nil
        state.currentTurnEndsAt = nil
        state.kingSwapHighlight = nil
    }

    private func indexOfPlayer(_ playerID: UUID) throws -> Int {
        guard let idx = state.players.firstIndex(where: { $0.id == playerID }) else {
            throw GameRuleError.invalidPlayer
        }
        return idx
    }

    private func validateTurn(for playerID: UUID) throws {
        guard state.currentPlayerID == playerID else {
            throw GameRuleError.invalidPlayer
        }
    }
}
