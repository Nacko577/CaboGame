package com.navitech.cabo.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hand: List<Card?> = emptyList(),
    val roundsToSkip: Int = 0
) {
    fun score(): Int = hand.filterNotNull().sumOf { it.rank.caboValue }
}
