package com.alex.cabogame.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Suit { HEARTS, DIAMONDS, CLUBS, SPADES }

@Serializable
enum class Rank(val value: Int) {
    ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
    EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13);

    val display: String get() = when (this) {
        ACE -> "A"; JACK -> "J"; QUEEN -> "Q"; KING -> "K"
        else -> value.toString()
    }

    val caboValue: Int get() = value
}

@Serializable
data class Card(
    val id: String = UUID.randomUUID().toString(),
    val suit: Suit,
    val rank: Rank
) {
    val shortName: String get() {
        val sym = when (suit) {
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
            Suit.SPADES -> "♠"
        }
        return "${rank.display}$sym"
    }
}

fun caboDeck(shuffled: Boolean = true): List<Card> {
    val deck = Suit.entries.flatMap { suit -> Rank.entries.map { rank -> Card(suit = suit, rank = rank) } }
    return if (shuffled) deck.shuffled() else deck
}
