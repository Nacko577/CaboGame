package com.navitech.cabo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DrawSource { DECK, DISCARD_TOP }

@Serializable
enum class TurnPhase {
    INITIAL_PEEK, WAITING_FOR_DRAW, WAITING_FOR_PLACEMENT_OR_DISCARD, WAITING_FOR_SPECIAL_RESOLUTION
}

@Serializable
sealed class SpecialAction {
    @Serializable @SerialName("none")
    object None : SpecialAction()

    @Serializable @SerialName("lookOwnCard")
    data class LookOwnCard(val playerID: String, val cardIndex: Int) : SpecialAction()

    @Serializable @SerialName("lookOtherCard")
    data class LookOtherCard(val targetPlayerID: String, val cardIndex: Int) : SpecialAction()

    @Serializable @SerialName("swapCards")
    data class SwapCards(
        val fromPlayerID: String,
        val fromCardIndex: Int,
        val toPlayerID: String,
        val toCardIndex: Int
    ) : SpecialAction()
}
