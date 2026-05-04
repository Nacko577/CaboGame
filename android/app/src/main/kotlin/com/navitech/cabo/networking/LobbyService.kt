package com.navitech.cabo.networking

import com.navitech.cabo.models.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class LobbyPeer(val id: String, val displayName: String)

interface LobbyServiceDelegate {
    fun onPeersUpdated(peers: List<LobbyPeer>)
    fun onMessageReceived(message: NetworkMessage, from: String)
    fun onConnectionStateChanged(text: String)
    /**
     * Called when the join code becomes available (synchronously for the
     * local service, asynchronously for the remote service after the server
     * confirms the lobby was created). Pass `null` to signal that no lobby is
     * currently being hosted.
     */
    fun onJoinCodeUpdated(code: String?) {}
}

interface LobbyService {
    var delegate: LobbyServiceDelegate?
    val joinCode: String?
    fun startHosting(displayName: String)
    fun startJoining(displayName: String, code: String)
    fun stop()
    fun send(message: NetworkMessage)
}

@Serializable
sealed class NetworkMessage {
    @Serializable @SerialName("lobbyState")
    data class LobbyState(val players: List<String>, val hostName: String) : NetworkMessage()

    @Serializable @SerialName("gameState")
    data class GameStateMsg(
        val state: GameState,
        /**
         * The host's epoch-millis clock at the moment this message was emitted.
         * Used by guests to re-base [GameState.initialPeekGraceEndsAt] onto
         * their own local clock, so countdowns stay synced even when the two
         * devices' system clocks are skewed.
         */
        val serverNowMillis: Long? = null
    ) : NetworkMessage()

    @Serializable @SerialName("playerAction")
    data class PlayerActionMsg(val action: PlayerNetworkAction) : NetworkMessage()

    @Serializable @SerialName("startGame")
    object StartGame : NetworkMessage()
}

@Serializable
sealed class PlayerNetworkAction {
    @Serializable @SerialName("initialPeek")
    data class InitialPeek(val playerID: String, val index: Int) : PlayerNetworkAction()

    @Serializable @SerialName("startNewGameRound")
    data class StartNewGameRound(val playerID: String) : PlayerNetworkAction()

    @Serializable @SerialName("setReadyForNewGame")
    data class SetReadyForNewGame(val playerID: String, val isReady: Boolean) : PlayerNetworkAction()

    @Serializable @SerialName("attemptMatchDiscard")
    data class AttemptMatchDiscard(val playerID: String, val index: Int) : PlayerNetworkAction()

    @Serializable @SerialName("draw")
    data class Draw(val playerID: String, val source: DrawSource) : PlayerNetworkAction()

    @Serializable @SerialName("replace")
    data class Replace(val playerID: String, val index: Int) : PlayerNetworkAction()

    @Serializable @SerialName("discardForEffect")
    data class DiscardForEffect(val playerID: String) : PlayerNetworkAction()

    @Serializable @SerialName("resolveSpecial")
    data class ResolveSpecial(val playerID: String, val action: SpecialAction) : PlayerNetworkAction()

    @Serializable @SerialName("callCabo")
    data class CallCabo(val playerID: String) : PlayerNetworkAction()
}
