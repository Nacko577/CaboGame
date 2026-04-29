import Foundation

@MainActor
final class GameViewModel: ObservableObject {
    @Published var playerName: String = ""
    @Published var joinCodeInput: String = ""
    @Published var statusText: String = "Not connected"
    @Published var peers: [LobbyPeer] = []
    @Published var localPlayerID: UUID?
    @Published var hostedCode: String?
    @Published var gameState = GameState()
    @Published var initialPeekedOwnIndices: Set<Int> = []
    @Published var jackPeekedOwnIndex: Int?
    @Published var queenPeekedOpponentPlayerID: UUID?
    @Published var queenPeekedOpponentIndex: Int?
    @Published var initialPeekSecondsRemaining: Int?
    @Published var lastDrawnCard: Card?
    @Published var lastReplacedIncomingCard: Card?
    @Published var matchDiscardStatusText: String?
    @Published var matchAttemptOwnCard: Card?
    @Published var matchAttemptTopDiscardCard: Card?
    @Published var lastError: String?

    private var lobby: LobbyService
    private var engine = CaboGameEngine()
    private var initialPeekGraceTask: Task<Void, Never>?
    private var specialPeekClearTask: Task<Void, Never>?

    init(lobby: LobbyService = LocalLobbyService()) {
        self.lobby = lobby
        self.lobby.delegate = self
    }

    func hostLobby() {
        do {
            try lobby.startHosting(displayName: validatedName())
            hostedCode = lobby.joinCode
            statusText = "Lobby hosted"
            resetForLobbyAsHost()
        } catch {
            lastError = error.localizedDescription
        }
    }

    func joinLobby() {
        do {
            try lobby.startJoining(displayName: validatedName(), code: joinCodeInput)
            hostedCode = nil
            statusText = "Joining lobby..."
        } catch {
            lastError = error.localizedDescription
        }
    }

    func leaveLobby() {
        lobby.stop()
        initialPeekGraceTask?.cancel()
        initialPeekGraceTask = nil
        specialPeekClearTask?.cancel()
        specialPeekClearTask = nil
        peers = []
        hostedCode = nil
        gameState = GameState()
    }

    func startGameAsHost() {
        do {
            try engine.startGame()
            gameState = engine.state
            clearTurnFeedback()
            initialPeekGraceTask?.cancel()
            initialPeekGraceTask = nil
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
        let id = engine.addPlayer(name: validatedName())
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
                let matched = try engine.attemptMatchDiscard(playerID: playerID, handIndex: index)
                gameState = engine.state
                matchDiscardStatusText = matched
                    ? "Matched discard. Card removed from your hand."
                    : "Wrong match. You will sit out your next turn."
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
            let matched = try engine.attemptMatchDiscard(playerID: playerID, handIndex: index)
            if playerID == localPlayerID {
                matchDiscardStatusText = matched
                    ? "Matched discard. Card removed from your hand."
                    : "Wrong match. You will sit out your next turn."
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
                    return
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
                if self.localPlayerID == nil,
                   let me = incoming.players.first(where: { $0.name == self.validatedName() }) {
                    self.localPlayerID = me.id
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
            case .playerAction(let action):
                if self.isHost {
                    do {
                        try self.applyActionAsHost(action)
                        self.handleInitialPeekGraceIfNeeded()
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
}
