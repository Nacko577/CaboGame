import Foundation

enum LobbyTransport: String, CaseIterable, Identifiable {
    case online
    case local

    var id: String { rawValue }
    var label: String {
        switch self {
        case .online: return "Online"
        case .local: return "Same Wi-Fi"
        }
    }
}

@MainActor
final class GameViewModel: ObservableObject {
    @Published var playerName: String = ""
    @Published var joinCodeInput: String = ""
    @Published var statusText: String = "Not connected"
    @Published var peers: [LobbyPeer] = []
    @Published var localPlayerID: UUID?
    @Published var hostedCode: String?
    @Published var transport: LobbyTransport = .online
    @Published var gameState = GameState()
    @Published var initialPeekedOwnIndices: Set<Int> = []
    @Published var jackPeekedOwnIndex: Int?
    @Published var queenPeekedOpponentPlayerID: UUID?
    @Published var queenPeekedOpponentIndex: Int?
    @Published var initialPeekSecondsRemaining: Int?
    @Published var currentTurnSecondsRemaining: Int?
    @Published var lastDrawnCard: Card?
    @Published var lastReplacedIncomingCard: Card?
    @Published var matchDiscardStatusText: String?
    @Published var matchAttemptOwnCard: Card?
    @Published var matchAttemptTopDiscardCard: Card?
    @Published var lastError: String?

    private var lobby: LobbyService
    private var engine = CaboGameEngine()
    private var initialPeekGraceTask: Task<Void, Never>?
    private var turnTimerTask: Task<Void, Never>?
    private var specialPeekClearTask: Task<Void, Never>?
    private var matchStatusClearTask: Task<Void, Never>?
    private var pendingHostStart: Bool = false
    /// The displayName captured at the moment the user tapped Host or Join.
    /// Used to match ourselves in the host's player list, so editing the name
    /// field after entering the lobby doesn't break self-identification.
    private var sessionDisplayName: String?

    init(lobby: LobbyService? = nil) {
        let initial: LobbyService = lobby ?? RemoteLobbyService()
        self.lobby = initial
        self.lobby.delegate = self
    }

    func switchTransport(_ next: LobbyTransport) {
        guard next != transport else { return }
        leaveLobby()
        lobby.delegate = nil
        transport = next
        installLobbyService(for: next)
    }

    private func installLobbyService(for transport: LobbyTransport) {
        var new: LobbyService
        switch transport {
        case .online: new = RemoteLobbyService()
        case .local: new = LocalLobbyService()
        }
        new.delegate = self
        lobby = new
    }

    func hostLobby() {
        do {
            let name = validatedName()
            sessionDisplayName = name
            pendingHostStart = true
            try lobby.startHosting(displayName: name)
            statusText = "Creating lobby..."
        } catch {
            pendingHostStart = false
            sessionDisplayName = nil
            lastError = error.localizedDescription
        }
    }

    func joinLobby() {
        do {
            let name = validatedName()
            sessionDisplayName = name
            try lobby.startJoining(displayName: name, code: joinCodeInput)
            hostedCode = nil
            statusText = "Joining lobby..."
        } catch {
            sessionDisplayName = nil
            lastError = error.localizedDescription
        }
    }

    func leaveLobby() {
        lobby.stop()
        initialPeekGraceTask?.cancel()
        initialPeekGraceTask = nil
        turnTimerTask?.cancel()
        turnTimerTask = nil
        currentTurnSecondsRemaining = nil
        specialPeekClearTask?.cancel()
        specialPeekClearTask = nil
        matchStatusClearTask?.cancel()
        matchStatusClearTask = nil
        peers = []
        hostedCode = nil
        pendingHostStart = false
        sessionDisplayName = nil
        localPlayerID = nil
        gameState = GameState()
    }

    func startGameAsHost() {
        do {
            try engine.startGame()
            gameState = engine.state
            clearTurnFeedback()
            initialPeekGraceTask?.cancel()
            initialPeekGraceTask = nil
            turnTimerTask?.cancel()
            turnTimerTask = nil
            currentTurnSecondsRemaining = nil
            // Host must start the countdown immediately when the game loads,
            // otherwise the host's timer only kicks in after the first guest
            // action, making it appear shorter than the guests' timer.
            handleInitialPeekGraceIfNeeded()
            handleTurnTimerIfNeeded()
            try lobby.send(.gameState(gameState))
            try lobby.send(.startGame)
        } catch {
            lastError = error.localizedDescription
        }
    }

    func startNewGameRound() {
        guard let playerID = localPlayerID else { return }
        do {
            if isHost {
                gameState.rematchRequestedByHost = true
                gameState.readyPlayerIDs = [playerID]
                engine.load(state: gameState)
                try lobby.send(.gameState(gameState))
                try maybeStartGameWhenAllReady()
            } else {
                try lobby.send(.playerAction(.startNewGameRound(playerID: playerID)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func toggleReadyForNewGame() {
        guard let playerID = localPlayerID else { return }
        let willBeReady = !gameState.readyPlayerIDs.contains(playerID)
        do {
            if isHost {
                if willBeReady {
                    gameState.readyPlayerIDs.insert(playerID)
                } else {
                    gameState.readyPlayerIDs.remove(playerID)
                }
                engine.load(state: gameState)
                try lobby.send(.gameState(gameState))
                try maybeStartGameWhenAllReady()
            } else {
                try lobby.send(.playerAction(.setReadyForNewGame(playerID: playerID, isReady: willBeReady)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func addLocalPlayerAsHost() {
        let id = engine.addPlayer(name: sessionDisplayName ?? validatedName())
        localPlayerID = id
    }

    func addRemotePlayer(name: String) {
        _ = engine.addPlayer(name: name)
    }

    func draw(from source: DrawSource) {
        guard let playerID = localPlayerID else { return }
        do {
            if isHost {
                let drawn = try engine.drawCard(for: playerID, source: source)
                lastDrawnCard = drawn
                lastReplacedIncomingCard = nil
                gameState = engine.state
                try lobby.send(.gameState(gameState))
            } else {
                try lobby.send(.playerAction(.draw(playerID: playerID, source: source)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func attemptMatchDiscard(withOwnCardAt index: Int) {
        guard let playerID = localPlayerID else { return }
        do {
            if let me = gameState.players.first(where: { $0.id == playerID }),
               me.hand.indices.contains(index),
               let selected = me.hand[index],
               let topDiscard = gameState.discardPile.last {
                matchAttemptOwnCard = selected
                matchAttemptTopDiscardCard = topDiscard
            }
            if isHost {
                let wasCurrentTurn = gameState.currentPlayerID == playerID
                let matched = try engine.attemptMatchDiscard(playerID: playerID, handIndex: index)
                gameState = engine.state
                matchDiscardStatusText = matchStatusText(matched: matched, wasCurrentTurn: wasCurrentTurn)
                scheduleMatchDiscardClear()
                try lobby.send(.gameState(gameState))
            } else {
                try lobby.send(.playerAction(.attemptMatchDiscard(playerID: playerID, index: index)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func peekInitialCard(at index: Int) {
        guard let playerID = localPlayerID else { return }
        do {
            if isHost {
                _ = try engine.peekInitialCard(for: playerID, at: index)
                initialPeekedOwnIndices.insert(index)
                gameState = engine.state
                try lobby.send(.gameState(gameState))
            } else {
                initialPeekedOwnIndices.insert(index)
                try lobby.send(.playerAction(.initialPeek(playerID: playerID, index: index)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func replaceWithDrawnCard(at index: Int) {
        guard let playerID = localPlayerID else { return }
        do {
            if isHost {
                let incoming = gameState.pendingDraw?.card
                try engine.replaceCard(for: playerID, at: index)
                gameState = engine.state
                if let incoming {
                    lastReplacedIncomingCard = incoming
                    lastDrawnCard = incoming
                }
                try lobby.send(.gameState(gameState))
            } else {
                try lobby.send(.playerAction(.replace(playerID: playerID, index: index)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func discardForEffect() {
        guard let playerID = localPlayerID else { return }
        do {
            if isHost {
                _ = try engine.discardDrawnCardAndUseEffect(for: playerID)
                gameState = engine.state
                try lobby.send(.gameState(gameState))
            } else {
                try lobby.send(.playerAction(.discardForEffect(playerID: playerID)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func resolveSpecial(_ action: SpecialAction) {
        guard let playerID = localPlayerID else { return }
        do {
            if case .lookOwnCard(_, let idx) = action {
                jackPeekedOwnIndex = idx
                scheduleSpecialPeekClear()
            }
            if case .lookOtherCard(let targetID, let idx) = action {
                queenPeekedOpponentPlayerID = targetID
                queenPeekedOpponentIndex = idx
                scheduleSpecialPeekClear()
            }

            if isHost {
                try engine.resolveSpecialAction(for: playerID, action: action)
                gameState = engine.state
                try lobby.send(.gameState(gameState))
            } else {
                try lobby.send(.playerAction(.resolveSpecial(playerID: playerID, action: action)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    func callCabo() {
        guard let playerID = localPlayerID else { return }
        do {
            if isHost {
                try engine.callCabo(for: playerID)
                gameState = engine.state
                try lobby.send(.gameState(gameState))
            } else {
                try lobby.send(.playerAction(.callCabo(playerID: playerID)))
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    private func validatedName() -> String {
        let trimmed = playerName.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "Player" : trimmed
    }

    /// Locate the local player in an incoming `GameState`. The user might have
    /// edited the name field after joining, so we prefer the displayName captured
    /// at host/join time, then fall back to the live one. We do a case-insensitive,
    /// whitespace-trimmed comparison to be forgiving.
    private func identifyLocalPlayer(in incoming: GameState) -> Player? {
        let candidates: [String] = [
            sessionDisplayName,
            validatedName()
        ].compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
         .filter { !$0.isEmpty }

        for candidate in candidates {
            if let match = incoming.players.first(where: {
                $0.name.trimmingCharacters(in: .whitespacesAndNewlines)
                    .caseInsensitiveCompare(candidate) == .orderedSame
            }) {
                return match
            }
        }
        return nil
    }

    private func resetForLobbyAsHost() {
        engine = CaboGameEngine()
        addLocalPlayerAsHost()
        gameState = engine.state
        clearTurnFeedback()
    }

    private var isHost: Bool {
        hostedCode != nil
    }

    private func clearTurnFeedback() {
        lastDrawnCard = nil
        lastReplacedIncomingCard = nil
        initialPeekSecondsRemaining = nil
    }

    private func clearSpecialPeekFeedback() {
        jackPeekedOwnIndex = nil
        queenPeekedOpponentPlayerID = nil
        queenPeekedOpponentIndex = nil
        specialPeekClearTask?.cancel()
        specialPeekClearTask = nil
    }

    private func scheduleSpecialPeekClear(after seconds: UInt64 = 5) {
        specialPeekClearTask?.cancel()
        specialPeekClearTask = Task { @MainActor [weak self] in
            guard let self else { return }
            try? await Task.sleep(nanoseconds: seconds * 1_000_000_000)
            if !Task.isCancelled {
                self.clearSpecialPeekFeedback()
            }
        }
    }

    private func matchStatusText(matched: Bool, wasCurrentTurn: Bool) -> String {
        if matched {
            return "Correct match! Card removed from your hand."
        }
        return wasCurrentTurn
            ? "Wrong match. Your turn is skipped."
            : "Wrong match. You will sit out your next turn."
    }

    private func clearMatchDiscardFeedback() {
        matchDiscardStatusText = nil
        matchAttemptOwnCard = nil
        matchAttemptTopDiscardCard = nil
        matchStatusClearTask?.cancel()
        matchStatusClearTask = nil
    }

    private func scheduleMatchDiscardClear(after seconds: UInt64 = 5) {
        matchStatusClearTask?.cancel()
        matchStatusClearTask = Task { @MainActor [weak self] in
            guard let self else { return }
            try? await Task.sleep(nanoseconds: seconds * 1_000_000_000)
            if !Task.isCancelled {
                self.clearMatchDiscardFeedback()
            }
        }
    }

    private func applyActionAsHost(_ action: PlayerNetworkAction) throws {
        switch action {
        case .initialPeek(let playerID, let index):
            _ = try engine.peekInitialCard(for: playerID, at: index)
            if playerID == localPlayerID {
                initialPeekedOwnIndices.insert(index)
            }
        case .startNewGameRound(let playerID):
            guard playerID == localPlayerID else { break }
            gameState.rematchRequestedByHost = true
            gameState.readyPlayerIDs = [playerID]
            engine.load(state: gameState)
            try maybeStartGameWhenAllReady()
            return
        case .setReadyForNewGame(let playerID, let isReady):
            if isReady {
                gameState.readyPlayerIDs.insert(playerID)
            } else {
                gameState.readyPlayerIDs.remove(playerID)
            }
            engine.load(state: gameState)
            try maybeStartGameWhenAllReady()
            return
        case .attemptMatchDiscard(let playerID, let index):
            if playerID == localPlayerID,
               let me = gameState.players.first(where: { $0.id == playerID }),
               me.hand.indices.contains(index),
               let selected = me.hand[index],
               let topDiscard = gameState.discardPile.last {
                matchAttemptOwnCard = selected
                matchAttemptTopDiscardCard = topDiscard
            }
            let wasCurrentTurn = gameState.currentPlayerID == playerID
            let matched = try engine.attemptMatchDiscard(playerID: playerID, handIndex: index)
            if playerID == localPlayerID {
                matchDiscardStatusText = matchStatusText(matched: matched, wasCurrentTurn: wasCurrentTurn)
                scheduleMatchDiscardClear()
            }
        case .draw(let playerID, let source):
            let card = try engine.drawCard(for: playerID, source: source)
            if playerID == localPlayerID {
                lastDrawnCard = card
                lastReplacedIncomingCard = nil
            }
        case .replace(let playerID, let index):
            let incoming = engine.state.pendingDraw?.card
            try engine.replaceCard(for: playerID, at: index)
            if playerID == localPlayerID, let incoming {
                lastDrawnCard = incoming
                lastReplacedIncomingCard = incoming
            }
        case .discardForEffect(let playerID):
            _ = try engine.discardDrawnCardAndUseEffect(for: playerID)
        case .resolveSpecial(let playerID, let action):
            try engine.resolveSpecialAction(for: playerID, action: action)
        case .callCabo(let playerID):
            try engine.callCabo(for: playerID)
        }
        gameState = engine.state
        try lobby.send(.gameState(gameState))
    }

    private func maybeStartGameWhenAllReady() throws {
        guard isHost, gameState.rematchRequestedByHost else {
            try lobby.send(.gameState(gameState))
            return
        }
        let allPlayerIDs = Set(gameState.players.map(\.id))
        if allPlayerIDs.isSubset(of: gameState.readyPlayerIDs) && !allPlayerIDs.isEmpty {
            startGameAsHost()
        } else {
            try lobby.send(.gameState(gameState))
        }
    }

    private func handleInitialPeekGraceIfNeeded() {
        guard gameState.phase == .initialPeek,
              let graceEndsAt = gameState.initialPeekGraceEndsAt else {
            initialPeekSecondsRemaining = nil
            initialPeekGraceTask?.cancel()
            initialPeekGraceTask = nil
            return
        }

        let remaining = max(0, Int(ceil(graceEndsAt.timeIntervalSinceNow)))
        initialPeekSecondsRemaining = remaining

        if initialPeekGraceTask != nil { return }
        if remaining == 0 {
            // Timer ended already; host will advance state on its next update.
            return
        }

        initialPeekGraceTask = Task { @MainActor [weak self] in
            guard let self else { return }
            while !Task.isCancelled {
                guard self.gameState.phase == .initialPeek,
                      let graceEndsAt = self.gameState.initialPeekGraceEndsAt else {
                    self.initialPeekSecondsRemaining = nil
                    self.initialPeekGraceTask = nil
                    // The phase just flipped to main play; kick off the
                    // per-turn countdown so the UI starts ticking immediately.
                    self.handleTurnTimerIfNeeded()
                    return
                }

                let secs = max(0, Int(ceil(graceEndsAt.timeIntervalSinceNow)))
                self.initialPeekSecondsRemaining = secs
                if secs == 0 {
                    if self.isHost {
                        self.engine.load(state: self.gameState)
                        self.engine.beginMainTurnsAfterInitialPeekIfReady()
                        self.gameState = self.engine.state
                        do {
                            try self.lobby.send(.gameState(self.gameState))
                        } catch {
                            self.lastError = error.localizedDescription
                        }
                    }
                    self.initialPeekGraceTask = nil
                    self.handleTurnTimerIfNeeded()
                    return
                }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    /// Drives the per-turn countdown. Mirrors `handleInitialPeekGraceIfNeeded`:
    ///  - Updates `currentTurnSecondsRemaining` once a second.
    ///  - On the host, advances the turn (and re-broadcasts) when the
    ///    countdown reaches zero.
    ///  - The task is resilient to state updates: it re-reads the deadline
    ///    every tick, so if the engine resets the deadline mid-loop (after a
    ///    new turn begins), the task automatically picks up the new value.
    private func handleTurnTimerIfNeeded() {
        guard gameState.winnerID == nil,
              gameState.phase != .initialPeek,
              let deadline = gameState.currentTurnEndsAt else {
            currentTurnSecondsRemaining = nil
            turnTimerTask?.cancel()
            turnTimerTask = nil
            return
        }

        currentTurnSecondsRemaining = max(0, Int(ceil(deadline.timeIntervalSinceNow)))
        if turnTimerTask != nil { return }

        turnTimerTask = Task { @MainActor [weak self] in
            guard let self else { return }
            while !Task.isCancelled {
                guard self.gameState.winnerID == nil,
                      self.gameState.phase != .initialPeek,
                      let deadline = self.gameState.currentTurnEndsAt else {
                    self.currentTurnSecondsRemaining = nil
                    self.turnTimerTask = nil
                    return
                }

                let secs = max(0, Int(ceil(deadline.timeIntervalSinceNow)))
                self.currentTurnSecondsRemaining = secs

                if secs == 0 && self.isHost {
                    self.engine.load(state: self.gameState)
                    let advanced = self.engine.enforceTurnTimeoutIfNeeded()
                    if advanced {
                        self.gameState = self.engine.state
                        do {
                            try self.lobby.send(.gameState(self.gameState))
                        } catch {
                            self.lastError = error.localizedDescription
                        }
                    }
                }

                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    var isHostUser: Bool {
        isHost
    }

    var isLocalPlayerReadyForNewGame: Bool {
        guard let localPlayerID else { return false }
        return gameState.readyPlayerIDs.contains(localPlayerID)
    }
}

extension GameViewModel: LobbyServiceDelegate {
    nonisolated func lobbyServiceDidUpdatePeers(_ peers: [LobbyPeer]) {
        Task { @MainActor in
            self.peers = peers
            if self.hostedCode != nil {
                for peer in peers where !self.engine.state.players.contains(where: { $0.name == peer.displayName }) {
                    self.addRemotePlayer(name: peer.displayName)
                }
                self.gameState = self.engine.state
            }
        }
    }

    nonisolated func lobbyServiceDidReceive(message: NetworkMessage, from peerID: String) {
        Task { @MainActor in
            switch message {
            case .lobbyState:
                break
            case .gameState(let incoming):
                self.gameState = incoming
                self.engine.load(state: incoming)
                if self.localPlayerID == nil {
                    self.localPlayerID = self.identifyLocalPlayer(in: incoming)?.id
                }
                if incoming.currentPlayerID != self.localPlayerID,
                   incoming.phase != .initialPeek {
                    self.clearTurnFeedback()
                }
                if incoming.phase != .initialPeek {
                    self.initialPeekedOwnIndices = []
                }
                if incoming.winnerID != nil {
                    self.clearSpecialPeekFeedback()
                }
                self.handleInitialPeekGraceIfNeeded()
                self.handleTurnTimerIfNeeded()
            case .playerAction(let action):
                if self.isHost {
                    do {
                        try self.applyActionAsHost(action)
                        self.handleInitialPeekGraceIfNeeded()
                        self.handleTurnTimerIfNeeded()
                    } catch {
                        self.lastError = error.localizedDescription
                    }
                }
            case .startGame:
                self.statusText = "Game started"
            }
        }
    }

    nonisolated func lobbyServiceDidChangeConnectionState(_ text: String) {
        Task { @MainActor in
            self.statusText = text
        }
    }

    nonisolated func lobbyServiceDidUpdateJoinCode(_ code: String?) {
        Task { @MainActor in
            self.hostedCode = code
            if code != nil, self.pendingHostStart {
                self.pendingHostStart = false
                self.resetForLobbyAsHost()
                self.statusText = "Lobby hosted"
            }
        }
    }
}
