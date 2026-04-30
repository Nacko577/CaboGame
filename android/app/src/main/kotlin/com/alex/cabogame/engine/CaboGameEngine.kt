package com.alex.cabogame.engine

import com.alex.cabogame.models.*

class GameRuleError(message: String) : Exception(message)

class CaboGameEngine {
    companion object {
        /** Maximum length of a single player turn after the initial peek phase. */
        const val TURN_TIMEOUT_MILLIS: Long = 20_000L
    }

    var state: GameState = GameState()
        private set

    fun addPlayer(name: String): String {
        val player = Player(name = name)
        state = state.copy(players = state.players + player)
        return player.id
    }

    fun load(newState: GameState) {
        state = newState
    }

    fun startGame() {
        if (state.players.size < 2) throw GameRuleError("Need at least 2 players.")

        var players = state.players.map { it.copy(hand = List(4) { null }, roundsToSkip = 0) }
        val deck = caboDeck().toMutableList()

        for (slot in 0 until 4) {
            players = players.mapIndexed { idx, player ->
                val card = deck.removeLast()
                val hand = player.hand.toMutableList().also { it[slot] = card }
                player.copy(hand = hand)
            }
        }

        val firstDiscard = deck.removeLast()

        state = state.copy(
            players = players,
            deck = deck,
            discardPile = listOf(firstDiscard),
            currentPlayerIndex = 0,
            phase = TurnPhase.INITIAL_PEEK,
            pendingDraw = null,
            winnerID = null,
            caboCallerID = null,
            rematchRequestedByHost = false,
            readyPlayerIDs = emptySet(),
            hasStarted = true,
            playersFinishedInitialPeek = 0,
            initialPeekedIndicesByPlayerIndex = List(players.size) { emptyList() },
            // Start the initial peek grace timer immediately (iOS behavior).
            initialPeekGraceEndsAt = System.currentTimeMillis() + 15_000L,
            currentTurnEndsAt = null
        )
    }

    fun peekInitialCard(playerID: String, handIndex: Int): Card {
        if (state.phase != TurnPhase.INITIAL_PEEK) throw GameRuleError("This action is not valid right now.")

        val playerIndex = indexOfPlayer(playerID)
        val player = state.players[playerIndex]
        if (!player.hand.indices.contains(handIndex)) throw GameRuleError("Selected card index is invalid.")

        val alreadyPeeked = state.initialPeekedIndicesByPlayerIndex
            .getOrNull(playerIndex)
            ?.contains(handIndex) == true
        if (alreadyPeeked) throw GameRuleError("Selected card index is invalid.")

        val card = player.hand[handIndex] ?: throw GameRuleError("Selected card index is invalid.")

        val currentForPlayer = state.initialPeekedIndicesByPlayerIndex.getOrNull(playerIndex) ?: emptyList()
        val updatedForPlayer = currentForPlayer + handIndex

        val newInitialPeeked = state.initialPeekedIndicesByPlayerIndex.toMutableList().also { list ->
            // Be defensive: older/mismatched states might have an empty list.
            while (list.size < state.players.size) list.add(emptyList())
            list[playerIndex] = updatedForPlayer
        }

        var newState = state.copy(initialPeekedIndicesByPlayerIndex = newInitialPeeked)

        // Count completion when a player reaches 2 peeked cards.
        if (updatedForPlayer.size == 2) {
            newState = newState.copy(playersFinishedInitialPeek = newState.playersFinishedInitialPeek + 1)
        }

        state = newState
        return card
    }

    fun beginMainTurnsAfterInitialPeekIfReady(nowMillis: Long = System.currentTimeMillis()) {
        if (state.phase != TurnPhase.INITIAL_PEEK) return
        val graceEndsAt = state.initialPeekGraceEndsAt ?: return
        if (nowMillis < graceEndsAt) return

        state = state.copy(
            phase = TurnPhase.WAITING_FOR_DRAW,
            currentPlayerIndex = 0,
            initialPeekedIndicesByPlayerIndex = List(state.players.size) { emptyList() },
            playersFinishedInitialPeek = 0,
            initialPeekGraceEndsAt = null,
            currentTurnEndsAt = nowMillis + TURN_TIMEOUT_MILLIS
        )
    }

    /**
     * Forces the current player's turn to end because the per-turn timer
     * expired. Behavior depends on the phase the player got stuck in:
     *  - WAITING_FOR_DRAW: skip the turn outright.
     *  - WAITING_FOR_PLACEMENT_OR_DISCARD: discard the pending drawn card without
     *    applying its effect, then advance.
     *  - WAITING_FOR_SPECIAL_RESOLUTION: forfeit the J/Q/K effect (the card is
     *    already on the discard pile), advance.
     * Returns true if a turn was actually ended, false if there was nothing
     * to do (e.g. game over or initial peek phase).
     */
    fun enforceTurnTimeoutIfNeeded(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (state.winnerID != null) return false
        if (state.phase == TurnPhase.INITIAL_PEEK) return false
        val deadline = state.currentTurnEndsAt ?: return false
        if (nowMillis < deadline) return false

        if (state.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD) {
            val pending = state.pendingDraw
            if (pending != null) {
                state = state.copy(
                    discardPile = state.discardPile + pending.card,
                    pendingDraw = null
                )
            }
        } else if (state.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION) {
            // The J/Q/K is already on the discard pile from the prior
            // discardForEffect step; we just forfeit the effect.
            state = state.copy(pendingDraw = null)
        }
        endTurn(nowMillis)
        return true
    }

    fun attemptMatchDiscard(playerID: String, handIndex: Int): Boolean {
        if (state.winnerID != null) throw GameRuleError("Round already finished.")
        if (state.phase == TurnPhase.INITIAL_PEEK) throw GameRuleError("This action is not valid right now.")
        val topDiscard = state.discardPile.lastOrNull() ?: throw GameRuleError("No card in discard pile.")

        val playerIndex = indexOfPlayer(playerID)
        val player = state.players[playerIndex]
        if (!player.hand.indices.contains(handIndex)) throw GameRuleError("Selected card index is invalid.")
        val selected = player.hand[handIndex] ?: throw GameRuleError("Selected card index is invalid.")

        return if (selected.rank == topDiscard.rank) {
            val newHand = player.hand.toMutableList().also { it[handIndex] = null }
            val newPlayers = state.players.toMutableList().also {
                it[playerIndex] = player.copy(hand = newHand)
            }
            state = state.copy(players = newPlayers, discardPile = state.discardPile + selected)
            true
        } else {
            // Failed match: end the matcher's *current* turn if it is theirs;
            // otherwise (an off-turn match attempt) make them sit out their next
            // full turn.
            if (state.currentPlayerIndex == playerIndex) {
                // Discard any pending drawn card (without applying its effect)
                // so deck/discard counts stay consistent before advancing turn.
                val pending = state.pendingDraw
                if (pending != null) {
                    state = state.copy(
                        discardPile = state.discardPile + pending.card,
                        pendingDraw = null
                    )
                }
                endTurn()
            } else {
                val newPlayers = state.players.toMutableList().also {
                    it[playerIndex] = player.copy(roundsToSkip = player.roundsToSkip + 1)
                }
                state = state.copy(players = newPlayers)
            }
            false
        }
    }

    fun drawCard(playerID: String, source: DrawSource): Card {
        validateTurn(playerID)
        if (state.phase != TurnPhase.WAITING_FOR_DRAW) throw GameRuleError("This action is not valid right now.")

        val card = when (source) {
            DrawSource.DECK -> drawFromDeckInternal()
            DrawSource.DISCARD_TOP -> {
                state.discardPile.lastOrNull() ?: throw GameRuleError("No card in discard pile.")
                    .also { state = state.copy(discardPile = state.discardPile.dropLast(1)) }
                val top = state.discardPile.last()
                state = state.copy(discardPile = state.discardPile.dropLast(1))
                top
            }
        }

        state = state.copy(
            pendingDraw = PendingDraw(card, source),
            phase = TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD
        )
        return card
    }

    fun replaceCard(playerID: String, handIndex: Int) {
        validateTurn(playerID)
        if (state.phase != TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD) throw GameRuleError("This action is not valid right now.")
        val pending = state.pendingDraw ?: throw GameRuleError("No drawn card is pending.")

        val playerIndex = indexOfPlayer(playerID)
        val player = state.players[playerIndex]
        if (!player.hand.indices.contains(handIndex)) throw GameRuleError("Selected card index is invalid.")

        val replaced = player.hand[handIndex]
        val newHand = player.hand.toMutableList().also { it[handIndex] = pending.card }
        val newPlayers = state.players.toMutableList().also {
            it[playerIndex] = player.copy(hand = newHand)
        }
        val newDiscard = if (replaced != null) state.discardPile + replaced else state.discardPile

        state = state.copy(players = newPlayers, discardPile = newDiscard, pendingDraw = null)
        endTurn()
    }

    fun discardDrawnCardAndUseEffect(playerID: String): Rank {
        validateTurn(playerID)
        if (state.phase != TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD) throw GameRuleError("This action is not valid right now.")
        val pending = state.pendingDraw ?: throw GameRuleError("No drawn card is pending.")

        state = state.copy(discardPile = state.discardPile + pending.card, pendingDraw = null)

        when (pending.card.rank) {
            Rank.JACK, Rank.QUEEN, Rank.KING ->
                state = state.copy(phase = TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION)
            else -> endTurn()
        }
        return pending.card.rank
    }

    fun resolveSpecialAction(playerID: String, action: SpecialAction) {
        validateTurn(playerID)
        if (state.phase != TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION) throw GameRuleError("This action is not valid right now.")
        val top = state.discardPile.lastOrNull() ?: throw GameRuleError("No card in discard pile.")

        when {
            top.rank == Rank.JACK && action is SpecialAction.LookOwnCard -> {
                if (action.playerID != playerID) throw GameRuleError("This card has no special power.")
                val idx = indexOfPlayer(playerID)
                val player = state.players[idx]
                if (!player.hand.indices.contains(action.cardIndex) || player.hand[action.cardIndex] == null)
                    throw GameRuleError("Selected card index is invalid.")
            }
            top.rank == Rank.QUEEN && action is SpecialAction.LookOtherCard -> {
                if (action.targetPlayerID == playerID) throw GameRuleError("This card has no special power.")
                val idx = indexOfPlayer(action.targetPlayerID)
                val player = state.players[idx]
                if (!player.hand.indices.contains(action.cardIndex) || player.hand[action.cardIndex] == null)
                    throw GameRuleError("Selected card index is invalid.")
            }
            top.rank == Rank.KING && action is SpecialAction.SwapCards -> {
                if (action.fromPlayerID != playerID || action.toPlayerID == playerID)
                    throw GameRuleError("This card has no special power.")
                val fromIdx = indexOfPlayer(action.fromPlayerID)
                val toIdx = indexOfPlayer(action.toPlayerID)
                val fromPlayer = state.players[fromIdx]
                val toPlayer = state.players[toIdx]
                if (!fromPlayer.hand.indices.contains(action.fromCardIndex) || fromPlayer.hand[action.fromCardIndex] == null)
                    throw GameRuleError("Selected card index is invalid.")
                if (!toPlayer.hand.indices.contains(action.toCardIndex) || toPlayer.hand[action.toCardIndex] == null)
                    throw GameRuleError("Selected card index is invalid.")

                val newPlayers = state.players.toMutableList()
                val fromHand = fromPlayer.hand.toMutableList()
                val toHand = toPlayer.hand.toMutableList()
                val tmp = fromHand[action.fromCardIndex]
                fromHand[action.fromCardIndex] = toHand[action.toCardIndex]
                toHand[action.toCardIndex] = tmp
                newPlayers[fromIdx] = fromPlayer.copy(hand = fromHand)
                newPlayers[toIdx] = toPlayer.copy(hand = toHand)
                state = state.copy(players = newPlayers)
            }
            else -> throw GameRuleError("This card has no special power.")
        }

        endTurn()
    }

    fun callCabo(playerID: String) {
        validateTurn(playerID)
        if (state.phase != TurnPhase.WAITING_FOR_DRAW) throw GameRuleError("This action is not valid right now.")
        if (state.pendingDraw != null) throw GameRuleError("This action is not valid right now.")
        if (state.caboCallerID != null) throw GameRuleError("Cabo has already been called this round.")
        state = state.copy(caboCallerID = playerID)
        endTurn()
    }

    private fun drawFromDeckInternal(): Card {
        if (state.deck.isEmpty()) reshuffleDiscardIntoDeck()
        if (state.deck.isEmpty()) throw GameRuleError("Deck is empty.")
        val card = state.deck.last()
        state = state.copy(deck = state.deck.dropLast(1))
        return card
    }

    private fun reshuffleDiscardIntoDeck() {
        if (state.discardPile.size <= 1) throw GameRuleError("Deck is empty.")
        val keepTop = state.discardPile.last()
        state = state.copy(deck = state.discardPile.dropLast(1).shuffled(), discardPile = listOf(keepTop))
    }

    private fun endTurn(nowMillis: Long = System.currentTimeMillis()) {
        var next = state.currentPlayerIndex
        var safety = state.players.size
        val players = state.players.toMutableList()

        do {
            next = (next + 1) % players.size
            if (players[next].roundsToSkip > 0) {
                players[next] = players[next].copy(roundsToSkip = players[next].roundsToSkip - 1)
            } else {
                break
            }
            safety--
        } while (safety > 0)

        state = state.copy(players = players, currentPlayerIndex = next, phase = TurnPhase.WAITING_FOR_DRAW)

        if (state.caboCallerID != null && state.players[next].id == state.caboCallerID) {
            finishRound()
            return
        }

        // Reset the turn timer for the new active player.
        state = state.copy(currentTurnEndsAt = nowMillis + TURN_TIMEOUT_MILLIS)
    }

    private fun finishRound() {
        val winner = state.players.minByOrNull { it.score() }
        state = state.copy(
            winnerID = winner?.id,
            caboCallerID = null,
            phase = TurnPhase.WAITING_FOR_DRAW,
            pendingDraw = null,
            currentTurnEndsAt = null
        )
    }

    private fun indexOfPlayer(playerID: String): Int =
        state.players.indexOfFirst { it.id == playerID }
            .also { if (it == -1) throw GameRuleError("It is not this player's turn.") }

    private fun validateTurn(playerID: String) {
        if (state.currentPlayerID != playerID) throw GameRuleError("It is not this player's turn.")
    }
}
