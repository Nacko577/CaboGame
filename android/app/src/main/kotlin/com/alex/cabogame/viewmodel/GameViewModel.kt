package com.alex.cabogame.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alex.cabogame.engine.CaboGameEngine
import com.alex.cabogame.models.*
import com.alex.cabogame.networking.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private val lobby: LobbyService = LocalLobbyService(application)
    private var engine = CaboGameEngine()
    private var initialPeekGraceJob: Job? = null
    private var specialPeekClearJob: Job? = null

    val isHostUser: Boolean get() = hostedCode != null
    val isLocalPlayerReadyForNewGame: Boolean get() =
        localPlayerID?.let { gameState.readyPlayerIDs.contains(it) } ?: false

    init {
        lobby.delegate = object : LobbyServiceDelegate {
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
                        val incoming = message.state
                        gameState = incoming
                        engine.load(incoming)
                        if (localPlayerID == null) {
                            localPlayerID = incoming.players.firstOrNull { it.name == validatedName() }?.id
                        }
                        if (incoming.currentPlayerID != localPlayerID) clearTurnFeedback()
                        if (incoming.phase != TurnPhase.INITIAL_PEEK) initialPeekedOwnIndices = emptySet()
                        if (incoming.winnerID != null) clearSpecialPeekFeedback()
                        handleInitialPeekGraceIfNeeded()
                    }
                    is NetworkMessage.PlayerActionMsg -> {
                        if (isHostUser) {
                            try {
                                applyActionAsHost(message.action)
                                handleInitialPeekGraceIfNeeded()
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
        }
    }

    fun hostLobby() {
        lobby.startHosting(validatedName())
        hostedCode = lobby.joinCode
        statusText = "Lobby hosted"
        resetForLobbyAsHost()
    }

    fun joinLobby() {
        lobby.startJoining(validatedName(), joinCodeInput)
        hostedCode = null
        statusText = "Joining lobby..."
    }

    fun leaveLobby() {
        lobby.stop()
        initialPeekGraceJob?.cancel(); initialPeekGraceJob = null
        specialPeekClearJob?.cancel(); specialPeekClearJob = null
        peers = emptyList()
        hostedCode = null
        gameState = GameState()
    }

    fun startGameAsHost() {
        try {
            engine.startGame()
            gameState = engine.state
            clearTurnFeedback()
            initialPeekGraceJob?.cancel(); initialPeekGraceJob = null
            lobby.send(NetworkMessage.GameStateMsg(gameState))
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
                lobby.send(NetworkMessage.GameStateMsg(gameState))
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
                val matched = engine.attemptMatchDiscard(playerID, ownCardIndex)
                gameState = engine.state
                matchDiscardStatusText = if (matched)
                    "Matched discard. Card removed from your hand."
                else
                    "Wrong match. You will sit out your next turn."
                lobby.send(NetworkMessage.GameStateMsg(gameState))
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
            if (isHostUser) {
                engine.peekInitialCard(playerID, index)
                initialPeekedOwnIndices = initialPeekedOwnIndices + index
                gameState = engine.state
                lobby.send(NetworkMessage.GameStateMsg(gameState))
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
                lobby.send(NetworkMessage.GameStateMsg(gameState))
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
                lobby.send(NetworkMessage.GameStateMsg(gameState))
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
                lobby.send(NetworkMessage.GameStateMsg(gameState))
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
                lobby.send(NetworkMessage.GameStateMsg(gameState))
            } else {
                lobby.send(NetworkMessage.PlayerActionMsg(PlayerNetworkAction.CallCabo(playerID)))
            }
        } catch (e: Exception) {
            lastError = e.message
        }
    }

    private fun addRemotePlayer(name: String) { engine.addPlayer(name) }

    private fun resetForLobbyAsHost() {
        engine = CaboGameEngine()
        val id = engine.addPlayer(validatedName())
        localPlayerID = id
        gameState = engine.state
        clearTurnFeedback()
    }

    private fun validatedName(): String {
        val trimmed = playerName.trim()
        return if (trimmed.isEmpty()) "Player" else trimmed
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
            delay(4_000)
            if (isActive) clearSpecialPeekFeedback()
        }
    }

    private fun handleInitialPeekGraceIfNeeded() {
        val graceEndsAt = gameState.initialPeekGraceEndsAt
        if (gameState.phase != TurnPhase.INITIAL_PEEK ||
            gameState.playersFinishedInitialPeek < gameState.players.size ||
            graceEndsAt == null
        ) {
            initialPeekSecondsRemaining = null
            initialPeekGraceJob?.cancel(); initialPeekGraceJob = null
            return
        }

        val remaining = maxOf(0, ((graceEndsAt - System.currentTimeMillis()) / 1000).toInt())
        initialPeekSecondsRemaining = remaining

        if (!isHostUser || initialPeekGraceJob != null) return

        initialPeekGraceJob = viewModelScope.launch {
            while (isActive) {
                val secs = maxOf(0, ((graceEndsAt - System.currentTimeMillis()) / 1000).toInt())
                initialPeekSecondsRemaining = secs
                if (secs == 0) {
                    engine.load(gameState)
                    engine.beginMainTurnsAfterInitialPeekIfReady()
                    gameState = engine.state
                    lobby.send(NetworkMessage.GameStateMsg(gameState))
                    initialPeekGraceJob = null
                    return@launch
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
                val matched = engine.attemptMatchDiscard(action.playerID, action.index)
                if (action.playerID == localPlayerID) {
                    matchDiscardStatusText = if (matched) "Matched discard. Card removed from your hand."
                    else "Wrong match. You will sit out your next turn."
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
        lobby.send(NetworkMessage.GameStateMsg(gameState))
    }

    private fun maybeStartGameWhenAllReady() {
        if (!isHostUser || !gameState.rematchRequestedByHost) {
            lobby.send(NetworkMessage.GameStateMsg(gameState)); return
        }
        val allIDs = gameState.players.map { it.id }.toSet()
        if (allIDs.isNotEmpty() && allIDs.all { gameState.readyPlayerIDs.contains(it) }) {
            startGameAsHost()
        } else {
            lobby.send(NetworkMessage.GameStateMsg(gameState))
        }
    }

    override fun onCleared() {
        super.onCleared()
        lobby.stop()
    }
}
