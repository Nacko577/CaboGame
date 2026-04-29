package com.alex.cabogame.models

import kotlinx.serialization.Serializable

@Serializable
data class PendingDraw(val card: Card, val source: DrawSource)

@Serializable
data class GameState(
    val players: List<Player> = emptyList(),
    val deck: List<Card> = emptyList(),
    val discardPile: List<Card> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val phase: TurnPhase = TurnPhase.WAITING_FOR_DRAW,
    val pendingDraw: PendingDraw? = null,
    val winnerID: String? = null,
    val caboCallerID: String? = null,
    val rematchRequestedByHost: Boolean = false,
    val readyPlayerIDs: Set<String> = emptySet(),
    val hasStarted: Boolean = false,
    // Initial peek tracking per player (up to 2 cards each during INITIAL_PEEK).
    val initialPeekedIndicesByPlayerIndex: List<List<Int>> = emptyList(),
    val playersFinishedInitialPeek: Int = 0,
    val initialPeekGraceEndsAt: Long? = null  // epoch millis
) {
    val currentPlayerID: String? get() = players.getOrNull(currentPlayerIndex)?.id
}
