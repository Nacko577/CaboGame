package com.navitech.cabo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.navitech.cabo.engine.CaboGameEngine
import com.navitech.cabo.models.*
import com.navitech.cabo.networking.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

enum class LobbyTransport(val label: String) {
    ONLINE("Online"),
    LOCAL("Same Wi-Fi")
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    var playerName by mutableStateOf("")
    var joinCodeInput by mutableStateOf("")
    var statusText by mutableStateOf("Not connected")
        private set
    var peers by mutableStateOf<List<LobbyPeer>>(emptyList())
        private set
    var localPlayerID by mutableStateOf<String?>(null)
        private set
    var hostedCode by mutableStateOf<String?>(null)
        private set
    var transport by mutableStateOf(LobbyTransport.ONLINE)
        private set
    var gameState by mutableStateOf(GameState())
        private set
    var initialPeekedOwnIndices by mutableStateOf<Set<Int>>(emptySet())
        private set
    var jackPeekedOwnIndex by mutableStateOf<Int?>(null)
        private set
    var queenPeekedOpponentPlayerID by mutableStateOf<String?>(null)
        private set
    var queenPeekedOpponentIndex by mutableStateOf<Int?>(null)
        private set
    var initialPeekSecondsRemaining by mutableStateOf<Int?>(null)
        private set
    var currentTurnSecondsRemaining by mutableStateOf<Int?>(null)
        private set
    var lastDrawnCard by mutableStateOf<Card?>(null)
        private set
    var lastReplacedIncomingCard by mutableStateOf<Card?>(null)
        private set
    var matchDiscardStatusText by mutableStateOf<String?>(null)
        private set
    var matchAttemptOwnCard by mutableStateOf<Card?>(null)
        private set
    var matchAttemptTopDiscardCard by mutableStateOf<Card?>(null)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    private var lobby: LobbyService = RemoteLobbyService()
    private var engine = CaboGameEngine()
    private var initialPeekGraceJob: Job? = null
    private var turnTimerJob: Job? = null
    private var specialPeekClearJob: Job? = null
    private var matchStatusClearJob: Job? = null
    private var pendingHostStart: Boolean = false
    /**
     * The displayName captured at the moment the user tapped Host or Join.
     * Used to match ourselves in the host's player list, so editing the name
     * field after entering the lobby doesn't break self-identification.
     */
    private var sessionDisplayName: String? = null

    val isHostUser: Boolean get() = hostedCode != null
    val isLocalPlayerReadyForNewGame: Boolean get() =
        localPlayerID?.let { gameState.readyPlayerIDs.contains(it) } ?: false

    private val lobbyDelegate = object : LobbyServiceDelegate {
        override fun onPeersUpdated(peers: List<LobbyPeer>) {
            this@GameViewModel.peers = peers
            if (hostedCode != null) {
                for (peer in peers) {
                    if (engine.state.players.none { it.name == peer.displayName }) {
                        addRemotePlayer(peer.displayName)
                    }
                }
                gameState = engine.state
            }
        }

        override fun onMessageReceived(message: NetworkMessage, from: String) {
            when (message) {
                is NetworkMessage.LobbyState -> Unit
                is NetworkMessage.GameStateMsg -> {
                    val incoming = rebaseToLocalClock(message.state, message.serverNowMillis)
                    gameState = incoming
                    engine.load(incoming)
                    if (localPlayerID == null) {
                        localPlayerID = identifyLocalPlayer(incoming)?.id
                    }
                    if (incoming.phase == TurnPhase.INITIAL_PEEK) {
                        clearSpecialPeekFeedback()
                        val pid = localPlayerID ?: identifyLocalPlayer(incoming)?.id
                        if (pid != null) {
                            val pi = incoming.players.indexOfFirst { it.id == pid }
                            if (pi >= 0 && pi < incoming.initialPeekedIndicesByPlayerIndex.size) {
                                val serverPeeks = incoming.initialPeekedIndicesByPlayerIndex[pi].toSet()
                                initialPeekedOwnIndices = initialPeekedOwnIndices.union(serverPeeks)
                            }
                        }
                    }
                    if (incoming.phase != TurnPhase.INITIAL_PEEK &&
                        incoming.currentPlayerID != localPlayerID
                    ) {
                        clearTurnFeedback()
                    }
                    if (incoming.phase != TurnPhase.INITIAL_PEEK) initialPeekedOwnIndices = emptySet()
                    if (incoming.winnerID != null) clearSpecialPeekFeedback()
                    handleInitialPeekGraceIfNeeded()
                    handleTurnTimerIfNeeded()
                }
                is NetworkMessage.PlayerActionMsg -> {
                    if (isHostUser) {
                        try {
                            applyActionAsHost(message.action)
                            handleInitialPeekGraceIfNeeded()
                            handleTurnTimerIfNeeded()
                        } catch (e: Exception) {
                            lastError = e.message
                        }
                    }
                }
                is NetworkMessage.StartGame -> statusText = "Game started"
            }
        }

        override fun onConnectionStateChanged(text: String) {
            statusText = text
        }

        override fun onJoinCodeUpdated(code: String?) {
            hostedCode = code
            if (code != null && pendingHostStart) {
                pendingHostStart = false
                resetForLobbyAsHost()
                statusText = "Lobby hosted"
            }
        }
    }

    init {
        lobby.delegate = lobbyDelegate
    }

    fun switchTransport(next: LobbyTransport) {
        if (next == transport) return
        leaveLobby()
        lobby.delegate = null
        transport = next
        installLobbyService(next)
    }

    private fun installLobbyService(next: LobbyTransport) {
        val app = getApplication<Application>()
        lobby = when (next) {
            LobbyTransport.ONLINE -> RemoteLobbyService()
            LobbyTransport.LOCAL -> LocalLobbyService(app)
        }
        lobby.delegate = lobbyDelegate
    }

    fun hostLobby() {
        val name = validatedName()
        sessionDisplayName = name
        pendingHostStart = true
        lobby.startHosting(name)
        statusText = "Creating lobby..."
    }

    fun joinLobby() {
        val name = validatedName()
        sessionDisplayName = name
        lobby.startJoining(name, joinCodeInput)
        hostedCode = null
        statusText = "Joining lobby..."
    }

    fun leaveLobby() {
        lobby.stop()
        initialPeekGraceJob?.cancel(); initialPeekGraceJob = null
        turnTimerJob?.cancel(); turnTimerJob = null
        currentTurnSecondsRemaining = null
        specialPeekClearJob?.cancel(); specialPeekClearJob = null
        matchStatusClearJob?.cancel(); matchStatusClearJob = null
        peers = emptyList()
        hostedCode = null
        pendingHostStart = false
        sessionDisplayName = null
        localPlayerID = null
        gameState = GameState()
    }

    fun startGameAsHost() {
        try {
            engine.startGame()
            gameState = engine.state
            clearTurnFeedback()
            clearSpecialPeekFeedback()
            initialPeekGraceJob?.cancel(); initialPeekGraceJob = null
            turnTimerJob?.cancel(); turnTimerJob = null
            currentTurnSecondsRemaining = null
            // Host must start the countdown immediately when the game loads.
            handleInitialPeekGraceIfNeeded()
            handleTurnTimerIfNeeded()
            broadcastGameState()
            lobby.send(NetworkMessage.StartGame)
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun startNewGameRound() {
        val playerID = localPlayerID ?: return
        try {
            if (isHostUser) {
                gameState = gameState.copy(
                    rematchRequestedByHost = true,
                    readyPlayerIDs = setOf(playerID)
                )
                engine.load(gameState)
                maybeStartGameWhenAllReady()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.StartNewGameRound(playerID)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun toggleReadyForNewGame() {
        val playerID = localPlayerID ?: return
        val willBeReady = !gameState.readyPlayerIDs.contains(playerID)
        try {
            if (isHostUser) {
                gameState = if (willBeReady) {
                    gameState.copy(readyPlayerIDs = gameState.readyPlayerIDs + playerID)
                } else {
                    gameState.copy(readyPlayerIDs = gameState.readyPlayerIDs - playerID)
                }
                engine.load(gameState)
                maybeStartGameWhenAllReady()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.SetReadyForNewGame(playerID, willBeReady)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun draw(source: DrawSource) {
        val playerID = localPlayerID ?: return
        try {
            if (isHostUser) {
                val drawn = engine.drawCard(playerID, source)
                lastDrawnCard = drawn
                lastReplacedIncomingCard = null
                gameState = engine.state
                broadcastGameState()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.Draw(playerID, source)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun attemptMatchDiscard(ownCardIndex: Int) {
        val playerID = localPlayerID ?: return
        try {
            val me = gameState.players.firstOrNull { it.id == playerID }
            if (me != null && me.hand.indices.contains(ownCardIndex)) {
                matchAttemptOwnCard = me.hand[ownCardIndex]
                matchAttemptTopDiscardCard = gameState.discardPile.lastOrNull()
            }
            if (isHostUser) {
                val wasCurrentTurn = gameState.currentPlayerID == playerID
                val matched = engine.attemptMatchDiscard(playerID, ownCardIndex)
                gameState = engine.state
                matchDiscardStatusText = matchStatusText(matched, wasCurrentTurn)
                scheduleMatchDiscardClear()
                broadcastGameState()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.AttemptMatchDiscard(playerID, ownCardIndex)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun peekInitialCard(index: Int) {
        val playerID = localPlayerID ?: return
        try {
            // iOS behavior: during INITIAL_PEEK, peeking is not restricted by "whose turn".
            // Only enforce local remaining peeks + timer not ended.
            if (gameState.phase != TurnPhase.INITIAL_PEEK) return
            val graceEndsAt = gameState.initialPeekGraceEndsAt ?: return
            if (System.currentTimeMillis() >= graceEndsAt) return
            if (initialPeekSecondsRemaining != null && initialPeekSecondsRemaining == 0) return
            val remaining = maxOf(0, 2 - initialPeekedOwnIndices.size)
            if (remaining <= 0) return

            val me = gameState.players.firstOrNull { it.id == playerID }
            if (me?.hand?.getOrNull(index) == null) return

            if (isHostUser) {
                engine.peekInitialCard(playerID, index)
                initialPeekedOwnIndices = initialPeekedOwnIndices + index
                gameState = engine.state
                broadcastGameState()
            } else {
                initialPeekedOwnIndices = initialPeekedOwnIndices + index
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.InitialPeek(playerID, index)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun replaceWithDrawnCard(index: Int) {
        val playerID = localPlayerID ?: return
        try {
            if (isHostUser) {
                val incoming = gameState.pendingDraw?.card
                engine.replaceCard(playerID, index)
                gameState = engine.state
                if (incoming != null) { lastReplacedIncomingCard = incoming; lastDrawnCard = incoming }
                broadcastGameState()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.Replace(playerID, index)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun discardForEffect() {
        val playerID = localPlayerID ?: return
        try {
            if (isHostUser) {
                engine.discardDrawnCardAndUseEffect(playerID)
                gameState = engine.state
                broadcastGameState()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.DiscardForEffect(playerID)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun resolveSpecial(action: SpecialAction) {
        val playerID = localPlayerID ?: return
        try {
            if (action is SpecialAction.LookOwnCard) { jackPeekedOwnIndex = action.cardIndex; scheduleSpecialPeekClear() }
            if (action is SpecialAction.LookOtherCard) {
                queenPeekedOpponentPlayerID = action.targetPlayerID
                queenPeekedOpponentIndex = action.cardIndex
                scheduleSpecialPeekClear()
            }
            if (isHostUser) {
                engine.resolveSpecialAction(playerID, action)
                gameState = engine.state
                broadcastGameState()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.ResolveSpecial(playerID, action)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    fun callCabo() {
        val playerID = localPlayerID ?: return
        try {
            if (isHostUser) {
                engine.callCabo(playerID)
                gameState = engine.state
                broadcastGameState()
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.CallCabo(playerID)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    private fun addRemotePlayer(name: String) { engine.addPlayer(name) }

    /**
     * Broadcast the current game state stamped with the host's wall-clock time.
     * Guests use the timestamp to re-base time-bound state (e.g. the initial
     * peek deadline) onto their own clock so countdowns stay synchronized
     * regardless of cross-device clock skew.
     */
    private fun broadcastGameState() {
        lobby.send(
            NetworkMessage.GameStateMsg(
                state = gameState,
                serverNowMillis = System.currentTimeMillis()
            )
        )
    }

    /**
     * Translate any host-clock timestamps inside [state] into the receiver's
     * local clock using [serverNowMillis] as a reference point. This keeps the
     * initial-peek countdown synchronized between devices even when their
     * system clocks are skewed. Hosts skip the translation since the timestamps
     * are already in their own local clock.
     */
    private fun rebaseToLocalClock(state: GameState, serverNowMillis: Long?): GameState {
        if (isHostUser || serverNowMillis == null) return state
        val nowLocal = System.currentTimeMillis()
        val rebasedPeek = state.initialPeekGraceEndsAt?.let { serverEnd ->
            nowLocal + (serverEnd - serverNowMillis)
        }
        val rebasedTurn = state.currentTurnEndsAt?.let { serverEnd ->
            nowLocal + (serverEnd - serverNowMillis)
        }
        return state.copy(
            initialPeekGraceEndsAt = rebasedPeek,
            currentTurnEndsAt = rebasedTurn
        )
    }

    private fun resetForLobbyAsHost() {
        engine = CaboGameEngine()
        val id = engine.addPlayer(sessionDisplayName ?: validatedName())
        localPlayerID = id
        gameState = engine.state
        clearTurnFeedback()
    }

    private fun validatedName(): String {
        val trimmed = playerName.trim()
        return if (trimmed.isEmpty()) "Player" else trimmed
    }

    /**
     * Locate the local player in an incoming [GameState]. The user might have
     * edited the name field after joining, so we prefer the displayName captured
     * at host/join time, then fall back to the live one. We do a case-insensitive,
     * whitespace-trimmed comparison to be forgiving.
     */
    private fun identifyLocalPlayer(incoming: GameState): Player? {
        val candidates = listOfNotNull(sessionDisplayName, validatedName())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        for (candidate in candidates) {
            val match = incoming.players.firstOrNull {
                it.name.trim().equals(candidate, ignoreCase = true)
            }
            if (match != null) return match
        }
        return null
    }

    private fun clearTurnFeedback() {
        lastDrawnCard = null
        lastReplacedIncomingCard = null
        initialPeekSecondsRemaining = null
    }

    private fun clearSpecialPeekFeedback() {
        jackPeekedOwnIndex = null
        queenPeekedOpponentPlayerID = null
        queenPeekedOpponentIndex = null
        specialPeekClearJob?.cancel(); specialPeekClearJob = null
    }

    private fun scheduleSpecialPeekClear() {
        specialPeekClearJob?.cancel()
        specialPeekClearJob = viewModelScope.launch {
            delay(5_000)
            if (isActive) clearSpecialPeekFeedback()
        }
    }

    private fun matchStatusText(matched: Boolean, wasCurrentTurn: Boolean): String =
        when {
            matched -> "Correct match! Card removed from your hand."
            wasCurrentTurn -> "Wrong match. Your turn is skipped."
            else -> "Wrong match. You will sit out your next turn."
        }

    private fun clearMatchDiscardFeedback() {
        matchDiscardStatusText = null
        matchAttemptOwnCard = null
        matchAttemptTopDiscardCard = null
        matchStatusClearJob?.cancel(); matchStatusClearJob = null
    }

    private fun scheduleMatchDiscardClear() {
        matchStatusClearJob?.cancel()
        matchStatusClearJob = viewModelScope.launch {
            delay(5_000)
            if (isActive) clearMatchDiscardFeedback()
        }
    }

    private fun handleInitialPeekGraceIfNeeded() {
        val graceEndsAt = gameState.initialPeekGraceEndsAt
        if (gameState.phase != TurnPhase.INITIAL_PEEK || graceEndsAt == null) {
            initialPeekSecondsRemaining = null
            initialPeekGraceJob?.cancel(); initialPeekGraceJob = null
            return
        }

        // Use ceil to avoid showing 0 too early (which can stall transition).
        val now = System.currentTimeMillis()
        initialPeekSecondsRemaining = maxOf(0, ceil((graceEndsAt - now) / 1000.0).toInt())

        // Start a countdown loop on every client; only the host will advance game state when it hits 0.
        if (initialPeekGraceJob != null) return

        initialPeekGraceJob = viewModelScope.launch {
            while (isActive) {
                val currentEndsAt = gameState.initialPeekGraceEndsAt
                if (gameState.phase != TurnPhase.INITIAL_PEEK || currentEndsAt == null) {
                    initialPeekSecondsRemaining = null
                    initialPeekGraceJob = null
                    // Host may have advanced the phase over the network before our
                    // local countdown reached 0; ensure the main turn timer starts.
                    handleTurnTimerIfNeeded()
                    return@launch
                }

                val nowMs = System.currentTimeMillis()
                val secs = maxOf(0, ceil((currentEndsAt - nowMs) / 1000.0).toInt())
                initialPeekSecondsRemaining = secs

                if (secs == 0) {
                    if (isHostUser) {
                        engine.load(gameState)
                        // Force transition check at grace end time.
                        engine.beginMainTurnsAfterInitialPeekIfReady(nowMillis = currentEndsAt)
                        gameState = engine.state
                        broadcastGameState()
                    }
                    initialPeekGraceJob = null
                    // The phase just flipped to main play; kick off the
                    // per-turn countdown so the UI starts ticking immediately.
                    handleTurnTimerIfNeeded()
                    return@launch
                }

                delay(1_000)
            }
        }
    }

    /**
     * Drives the per-turn countdown. Mirrors [handleInitialPeekGraceIfNeeded]:
     *  - Updates [currentTurnSecondsRemaining] once a second.
     *  - On the host, advances the turn (and re-broadcasts) when the
     *    countdown reaches zero.
     *  - The job is resilient to state updates: it re-reads the deadline
     *    every tick, so if the engine resets the deadline mid-loop (after a
     *    new turn begins), the job automatically picks up the new value.
     */
    private fun handleTurnTimerIfNeeded() {
        val deadline = gameState.currentTurnEndsAt
        if (gameState.winnerID != null ||
            gameState.phase == TurnPhase.INITIAL_PEEK ||
            deadline == null
        ) {
            currentTurnSecondsRemaining = null
            turnTimerJob?.cancel(); turnTimerJob = null
            return
        }

        val now = System.currentTimeMillis()
        currentTurnSecondsRemaining = maxOf(0, ceil((deadline - now) / 1000.0).toInt())

        if (turnTimerJob != null) return

        turnTimerJob = viewModelScope.launch {
            while (isActive) {
                val currentEndsAt = gameState.currentTurnEndsAt
                if (gameState.winnerID != null ||
                    gameState.phase == TurnPhase.INITIAL_PEEK ||
                    currentEndsAt == null
                ) {
                    currentTurnSecondsRemaining = null
                    turnTimerJob = null
                    return@launch
                }

                val nowMs = System.currentTimeMillis()
                val secs = maxOf(0, ceil((currentEndsAt - nowMs) / 1000.0).toInt())
                currentTurnSecondsRemaining = secs

                if (secs == 0 && isHostUser) {
                    engine.load(gameState)
                    val advanced = engine.enforceTurnTimeoutIfNeeded(nowMillis = currentEndsAt)
                    if (advanced) {
                        gameState = engine.state
                        broadcastGameState()
                    }
                }

                delay(1_000)
            }
        }
    }

    private fun applyActionAsHost(action: PlayerNetworkAction) {
        when (action) {
            is PlayerNetworkAction.InitialPeek -> {
                engine.peekInitialCard(action.playerID, action.index)
                if (action.playerID == localPlayerID) initialPeekedOwnIndices = initialPeekedOwnIndices + action.index
            }
            is PlayerNetworkAction.StartNewGameRound -> {
                if (action.playerID != localPlayerID) return
                gameState = gameState.copy(rematchRequestedByHost = true, readyPlayerIDs = setOf(action.playerID))
                engine.load(gameState)
                maybeStartGameWhenAllReady(); return
            }
            is PlayerNetworkAction.SetReadyForNewGame -> {
                gameState = if (action.isReady) {
                    gameState.copy(readyPlayerIDs = gameState.readyPlayerIDs + action.playerID)
                } else {
                    gameState.copy(readyPlayerIDs = gameState.readyPlayerIDs - action.playerID)
                }
                engine.load(gameState)
                maybeStartGameWhenAllReady(); return
            }
            is PlayerNetworkAction.AttemptMatchDiscard -> {
                if (action.playerID == localPlayerID) {
                    val me = gameState.players.firstOrNull { it.id == action.playerID }
                    matchAttemptOwnCard = me?.hand?.getOrNull(action.index)
                    matchAttemptTopDiscardCard = gameState.discardPile.lastOrNull()
                }
                val wasCurrentTurn = gameState.currentPlayerID == action.playerID
                val matched = engine.attemptMatchDiscard(action.playerID, action.index)
                if (action.playerID == localPlayerID) {
                    matchDiscardStatusText = matchStatusText(matched, wasCurrentTurn)
                    scheduleMatchDiscardClear()
                }
            }
            is PlayerNetworkAction.Draw -> {
                val card = engine.drawCard(action.playerID, action.source)
                if (action.playerID == localPlayerID) { lastDrawnCard = card; lastReplacedIncomingCard = null }
            }
            is PlayerNetworkAction.Replace -> {
                val incoming = engine.state.pendingDraw?.card
                engine.replaceCard(action.playerID, action.index)
                if (action.playerID == localPlayerID && incoming != null) {
                    lastDrawnCard = incoming; lastReplacedIncomingCard = incoming
                }
            }
            is PlayerNetworkAction.DiscardForEffect -> engine.discardDrawnCardAndUseEffect(action.playerID)
            is PlayerNetworkAction.ResolveSpecial -> engine.resolveSpecialAction(action.playerID, action.action)
            is PlayerNetworkAction.CallCabo -> engine.callCabo(action.playerID)
        }
        gameState = engine.state
        broadcastGameState()
    }

    private fun maybeStartGameWhenAllReady() {
        if (!isHostUser || !gameState.rematchRequestedByHost) {
            broadcastGameState(); return
        }
        val allIDs = gameState.players.map { it.id }.toSet()
        if (allIDs.isNotEmpty() && allIDs.all { gameState.readyPlayerIDs.contains(it) }) {
            startGameAsHost()
        } else {
            broadcastGameState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        lobby.stop()
    }
}
