package com.navitech.cabo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navitech.cabo.models.*
import com.navitech.cabo.ui.theme.CardBack
import com.navitech.cabo.ui.theme.GreenDark
import com.navitech.cabo.ui.theme.GreenDeep
import com.navitech.cabo.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun GameTableScreen(viewModel: GameViewModel, onLeaveGame: () -> Unit) {
    BackHandler(onBack = onLeaveGame)

    var selectedOwnIndex by remember { mutableIntStateOf(0) }
    var selectedOpponentIndex by remember { mutableIntStateOf(0) }
    var selectedOpponentID by remember { mutableStateOf<String?>(null) }

    val gameState = viewModel.gameState
    val localPlayerID = viewModel.localPlayerID
    val typedName = viewModel.playerName.trim().ifEmpty { "Player" }
    val resolvedLocalPlayer = gameState.players.firstOrNull { it.id == localPlayerID }
        ?: gameState.players.firstOrNull { it.name.equals(typedName, ignoreCase = true) }
        ?: gameState.players.lastOrNull()
    val localPlayer = resolvedLocalPlayer
    val resolvedLocalPlayerID = resolvedLocalPlayer?.id
    val opponents = gameState.players.filter { it.id != resolvedLocalPlayerID }
    val isMyTurn = gameState.currentPlayerID == resolvedLocalPlayerID
    val currentTurnName = gameState.players.firstOrNull { it.id == gameState.currentPlayerID }?.name ?: "Unknown"
    val activeSpecialRank = if (gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION)
        gameState.discardPile.lastOrNull()?.rank?.takeIf { it == Rank.JACK || it == Rank.QUEEN || it == Rank.KING }
    else null

    val specialPowerText: String? = when (activeSpecialRank) {
        Rank.KING -> "K: Swap one of your cards with any opponents card"
        Rank.JACK -> "J: Peek at one of your cards"
        Rank.QUEEN -> "Q: Peek at one of any opponents card"
        else -> null
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(gameState.phase, activeSpecialRank, viewModel.matchDiscardStatusText) {
        scrollState.scrollTo(0)
    }

    // Turn countdown is driven from authoritative [GameState.currentTurnEndsAt]
    // so it always renders on Android even if the ViewModel ticker misses a
    // snapshot update (Compose + state timing edge cases with nested jobs).
    var displayTurnSecs by remember { mutableIntStateOf(-1) }
    LaunchedEffect(gameState.currentTurnEndsAt, gameState.phase, gameState.winnerID) {
        if (gameState.winnerID != null ||
            gameState.phase == TurnPhase.INITIAL_PEEK ||
            gameState.currentTurnEndsAt == null
        ) {
            displayTurnSecs = -1
            return@LaunchedEffect
        }
        while (isActive) {
            val end = viewModel.gameState.currentTurnEndsAt
            if (viewModel.gameState.winnerID != null ||
                viewModel.gameState.phase == TurnPhase.INITIAL_PEEK ||
                end == null
            ) {
                displayTurnSecs = -1
                break
            }
            displayTurnSecs = maxOf(0, ceil((end - System.currentTimeMillis()) / 1000.0).toInt())
            delay(1000)
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val compactBoard = screenHeightDp < 750
    val boardHeightCapDp = if (compactBoard) 520 else 560
    val boardH = min(
        (screenHeightDp * (if (compactBoard) 0.66f else 0.70f)).toInt(),
        boardHeightCapDp
    ).dp
    val boardW = (screenWidthDp * 0.94f).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(GreenDark, GreenDeep)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 10.dp)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onLeaveGame) {
                    Text("Leave", color = Color.White)
                }
            }

            // Header HUD
            if (gameState.winnerID != null) {
                // Game is over; the round-result banner takes over.
            } else if (gameState.phase == TurnPhase.INITIAL_PEEK) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val remaining = maxOf(0, 2 - viewModel.initialPeekedOwnIndices.size)
                    StatusChip(
                        text = "Peeking",
                        color = Color(0xFFF2BF4D),
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        text = "Peek 2 cards (${remaining} left)",
                        color = Color(0xFFF2BF4D),
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(
                        text = "Time left: ${viewModel.initialPeekSecondsRemaining ?: 0}s",
                        color = Color(0xFFF2BF4D),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val headerText = if (isMyTurn) "Your Turn" else "${currentTurnName}'s turn"
                    val headerColor = if (isMyTurn) Color(0xFF4CAF50) else Color(0xFFF2BF4D)
                    StatusChip(headerText, color = headerColor, modifier = Modifier.weight(1f))
                    if (displayTurnSecs >= 0) {
                        val timerColor = if (displayTurnSecs <= 5) Color(0xFFE53935) else Color(0xFFF2BF4D)
                        StatusChip("${displayTurnSecs}s", color = timerColor, modifier = Modifier.weight(1f))
                    }
                    localPlayer?.let { me ->
                        if (me.roundsToSkip > 0) {
                            StatusChip(
                                "Skip ${me.roundsToSkip}",
                                color = Color(0xFFF44336),
                                modifier = Modifier.wrapContentWidth()
                            )
                        }
                    }
                }
            }

            // Match-result banner sits above the table so the player gets
            // immediate, prominent feedback about their match attempt.
            viewModel.matchDiscardStatusText?.let { matchStatus ->
                val isCorrect = matchStatus.startsWith("Correct", ignoreCase = true)
                StatusChip(
                    text = matchStatus,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Updated iOS-style board layout: N/S/E/W seats around a center table.
            val north = opponents.getOrNull(0)
            val east = opponents.getOrNull(1)
            val west = opponents.getOrNull(2)

            Box(
                modifier = Modifier
                    .width(boardW)
                    .height(boardH)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF15281E))
                    .border(2.dp, Color(0xFF2E8E67), RoundedCornerShape(24.dp))
            ) {
                    // Center piles
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Discard", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            val top = gameState.discardPile.lastOrNull()
                            TableCard(text = top?.shortName ?: "", isFaceUp = top != null, isSelected = false, isEmpty = top == null, width = 48.dp, height = 68.dp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Drawn", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            val drawnText = when {
                                isMyTurn && gameState.pendingDraw != null -> gameState.pendingDraw.card.shortName
                                viewModel.lastReplacedIncomingCard != null -> viewModel.lastReplacedIncomingCard!!.shortName
                                else -> null
                            }
                            TableCard(
                                text = drawnText ?: "",
                                isFaceUp = drawnText != null,
                                isSelected = drawnText != null && isMyTurn && gameState.pendingDraw != null,
                                isEmpty = drawnText == null,
                                width = 48.dp,
                                height = 68.dp
                            )
                        }
                    }

                    // North seat — cards toward top, name below (toward center)
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        player = north,
                        isLocal = false,
                        tableSeat = TableSeatPosition.North,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )

                    // South seat (local) — name above row (toward center)
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        player = localPlayer,
                        isLocal = true,
                        tableSeat = TableSeatPosition.South,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )

                    // East — inset from table rim; landscape stack; name +180° from prior 90°
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp),
                        player = east,
                        isLocal = false,
                        tableSeat = TableSeatPosition.East,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )

                    // West — inset from table rim
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp),
                        player = west,
                        isLocal = false,
                        tableSeat = TableSeatPosition.West,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )
            }

            // Action controls
            if (gameState.winnerID == null) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        if (gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION && isMyTurn && activeSpecialRank != null) {
                            // Active player resolving a J/Q/K effect: show only the
                            // contextual special-resolution controls, mirroring iOS.
                            val mySelectedCard = localPlayer?.hand?.getOrNull(selectedOwnIndex)
                            val selectedOpponentPlayer = selectedOpponentID?.let { id ->
                                gameState.players.firstOrNull { it.id == id }
                            }
                            val opponentSelectedCard = selectedOpponentPlayer?.hand?.getOrNull(selectedOpponentIndex)
                            when (activeSpecialRank) {
                                Rank.JACK -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ActionButton(
                                            label = "Peek",
                                            primary = true,
                                            enabled = mySelectedCard != null,
                                            onClick = {
                                                localPlayer?.let { me ->
                                                    viewModel.resolveSpecial(SpecialAction.LookOwnCard(me.id, selectedOwnIndex))
                                                }
                                            }
                                        )
                                        Text(
                                            "Choose a card",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Rank.QUEEN -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ActionButton(
                                            label = "Peek",
                                            primary = true,
                                            enabled = opponentSelectedCard != null,
                                            onClick = {
                                                selectedOpponentID?.let { targetID ->
                                                    viewModel.resolveSpecial(SpecialAction.LookOtherCard(targetID, selectedOpponentIndex))
                                                }
                                            }
                                        )
                                        Text(
                                            "Choose an opponent card",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Rank.KING -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ActionButton(
                                            label = "Swap",
                                            primary = true,
                                            enabled = mySelectedCard != null && opponentSelectedCard != null,
                                            onClick = {
                                                val targetID = selectedOpponentID ?: return@ActionButton
                                                localPlayer?.let { me ->
                                                    viewModel.resolveSpecial(
                                                        SpecialAction.SwapCards(me.id, selectedOwnIndex, targetID, selectedOpponentIndex)
                                                    )
                                                }
                                            }
                                        )
                                        Text(
                                            "Tap a card + opponent card, then swap.",
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        } else {
                            // Standard 2x3 button grid mirroring iOS:
                            // Row 1: Draw Deck | Draw Discard | Match
                            // Row 2: Replace   | Discard      | Cabo!
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ActionButton(
                                    label = "Draw Deck",
                                    primary = true,
                                    enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_DRAW,
                                    onClick = { viewModel.draw(DrawSource.DECK) }
                                )
                                ActionButton(
                                    label = "Draw Discard",
                                    enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_DRAW,
                                    onClick = { viewModel.draw(DrawSource.DISCARD_TOP) }
                                )
                                ActionButton(
                                    label = "Match",
                                    enabled = gameState.phase != TurnPhase.INITIAL_PEEK && localPlayer != null,
                                    onClick = { viewModel.attemptMatchDiscard(selectedOwnIndex) }
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ActionButton(
                                    label = "Replace",
                                    primary = true,
                                    enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD,
                                    onClick = { viewModel.replaceWithDrawnCard(selectedOwnIndex) }
                                )
                                ActionButton(
                                    label = "Discard",
                                    enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD,
                                    onClick = { viewModel.discardForEffect() }
                                )
                                ActionButton(
                                    label = "Cabo!",
                                    accentColor = Color(0xFFF2BF4D),
                                    enabled = isMyTurn &&
                                        gameState.phase == TurnPhase.WAITING_FOR_DRAW &&
                                        gameState.pendingDraw == null &&
                                        gameState.caboCallerID == null,
                                    onClick = { viewModel.callCabo() }
                                )
                            }
                        }
                    }
                }
            } else {
                // Rematch controls
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (viewModel.isHostUser) {
                            Button(onClick = { viewModel.startNewGameRound() }, modifier = Modifier.fillMaxWidth()) { Text("Start New Game") }
                            if (gameState.rematchRequestedByHost) {
                                Text("Waiting for all players to be ready...", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.toggleReadyForNewGame() },
                                enabled = gameState.rematchRequestedByHost,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (viewModel.isLocalPlayerReadyForNewGame) "Ready ✓" else "Ready") }
                            if (!gameState.rematchRequestedByHost) {
                                Text("Waiting for host to start a new game.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Status chips (winner / cabo / special-resolution help).
            // Match-result feedback is rendered above the table, not here.
            when {
                gameState.winnerID != null -> {
                    val winner = gameState.players.firstOrNull { it.id == gameState.winnerID }
                    if (winner != null) StatusChip("Winner: ${winner.name} (${winner.score()} points)", color = Color(0xFF26C6DA))
                }
                gameState.caboCallerID != null -> {
                    val caller = gameState.players.firstOrNull { it.id == gameState.caboCallerID }
                    if (caller != null) StatusChip("Cabo called by ${caller.name}. Final turns in progress...", color = Color(0xFFFF9800))
                }
                gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION && specialPowerText != null && isMyTurn -> {
                    // Only the player resolving the special should see the
                    // explanation text; opponents shouldn't be told what the
                    // active card is.
                    StatusChip(specialPowerText, color = Color(0xFF2EBF8C))
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private enum class TableSeatPosition {
    North, South, East, West
}

@Composable
private fun PortraitHandCards(
    layoutHorizontal: Boolean,
    player: Player,
    isLocal: Boolean,
    viewModel: GameViewModel,
    selectedOwnIndex: Int,
    onSelectOwn: (Int) -> Unit,
    selectedOpponentID: String?,
    selectedOpponentIndex: Int,
    onSelectOpponent: (String, Int) -> Unit,
    cardWidth: Dp = 32.dp,
    cardHeight: Dp = 46.dp,
    faceSymbolRotationDegrees: Float = 0f,
) {
    val hand = player.hand.take(4)
    if (layoutHorizontal) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            hand.forEachIndexed { idx, card ->
                val revealedBase = if (isLocal) {
                    viewModel.gameState.phase == TurnPhase.INITIAL_PEEK && viewModel.initialPeekedOwnIndices.contains(idx) ||
                        viewModel.jackPeekedOwnIndex == idx
                } else {
                    viewModel.queenPeekedOpponentPlayerID == player.id && viewModel.queenPeekedOpponentIndex == idx
                }
                val revealed = revealedBase || viewModel.gameState.winnerID != null
                val kingSwapOutline = viewModel.gameState.kingSwapHighlight?.let { h ->
                    (player.id == h.fromPlayerID && idx == h.fromHandIndex) ||
                        (player.id == h.toPlayerID && idx == h.toHandIndex)
                } ?: false
                TableCard(
                    text = if (revealed && card != null) card.shortName else "",
                    isFaceUp = revealed && card != null,
                    isSelected = if (isLocal) selectedOwnIndex == idx else (selectedOpponentID == player.id && selectedOpponentIndex == idx),
                    kingSwapOutline = kingSwapOutline,
                    isEmpty = card == null,
                    width = cardWidth,
                    height = cardHeight,
                    onTap = {
                        if (isLocal) onSelectOwn(idx) else onSelectOpponent(player.id, idx)
                    },
                    onDoubleTap = if (isLocal) {
                        { viewModel.peekInitialCard(idx) }
                    } else null,
                    faceSymbolRotationDegrees = faceSymbolRotationDegrees,
                )
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            hand.forEachIndexed { idx, card ->
                val revealedBase = if (isLocal) {
                    viewModel.gameState.phase == TurnPhase.INITIAL_PEEK && viewModel.initialPeekedOwnIndices.contains(idx) ||
                        viewModel.jackPeekedOwnIndex == idx
                } else {
                    viewModel.queenPeekedOpponentPlayerID == player.id && viewModel.queenPeekedOpponentIndex == idx
                }
                val revealed = revealedBase || viewModel.gameState.winnerID != null
                val kingSwapOutline = viewModel.gameState.kingSwapHighlight?.let { h ->
                    (player.id == h.fromPlayerID && idx == h.fromHandIndex) ||
                        (player.id == h.toPlayerID && idx == h.toHandIndex)
                } ?: false
                TableCard(
                    text = if (revealed && card != null) card.shortName else "",
                    isFaceUp = revealed && card != null,
                    isSelected = if (isLocal) selectedOwnIndex == idx else (selectedOpponentID == player.id && selectedOpponentIndex == idx),
                    kingSwapOutline = kingSwapOutline,
                    isEmpty = card == null,
                    width = cardWidth,
                    height = cardHeight,
                    onTap = {
                        if (isLocal) onSelectOwn(idx) else onSelectOpponent(player.id, idx)
                    },
                    onDoubleTap = if (isLocal) {
                        { viewModel.peekInitialCard(idx) }
                    } else null,
                    faceSymbolRotationDegrees = faceSymbolRotationDegrees,
                )
            }
        }
    }
}

@Composable
private fun SeatRow(
    modifier: Modifier = Modifier,
    player: Player?,
    isLocal: Boolean,
    tableSeat: TableSeatPosition,
    viewModel: GameViewModel,
    selectedOwnIndex: Int,
    onSelectOwn: (Int) -> Unit,
    selectedOpponentID: String?,
    selectedOpponentIndex: Int,
    onSelectOpponent: (String, Int) -> Unit
) {
    if (player == null) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Empty", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
        }
        return
    }
    val isTurn = viewModel.gameState.currentPlayerID == player.id
    val nameColor = if (isLocal) Color(0xFFF2BF4D) else if (isTurn) Color(0xFF4CAF50) else Color.White
    val label = if (isLocal) "You" else player.name
    val seatCardW = 32.dp
    val seatCardH = 46.dp
    val (cardW, cardH) = when (tableSeat) {
        TableSeatPosition.East, TableSeatPosition.West -> seatCardH to seatCardW // landscape: long edge toward table center
        else -> seatCardW to seatCardH
    }
    val faceSymbolRotation = when (tableSeat) {
        TableSeatPosition.East -> 90f
        TableSeatPosition.West -> -90f
        else -> 0f
    }
    @Composable
    fun seatName(rotMod: Modifier) {
        Text(
            label,
            color = nameColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = rotMod
        )
    }
    val cards: @Composable () -> Unit = {
        PortraitHandCards(
            layoutHorizontal = tableSeat == TableSeatPosition.North || tableSeat == TableSeatPosition.South,
            player = player,
            isLocal = isLocal,
            viewModel = viewModel,
            selectedOwnIndex = selectedOwnIndex,
            onSelectOwn = onSelectOwn,
            selectedOpponentID = selectedOpponentID,
            selectedOpponentIndex = selectedOpponentIndex,
            onSelectOpponent = onSelectOpponent,
            cardWidth = cardW,
            cardHeight = cardH,
            faceSymbolRotationDegrees = faceSymbolRotation,
        )
    }
    when (tableSeat) {
        TableSeatPosition.North -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            cards()
            seatName(Modifier)
        }
        TableSeatPosition.South -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            seatName(Modifier)
            cards()
        }
        TableSeatPosition.East -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            seatName(Modifier.rotate(270f))
            cards()
        }
        TableSeatPosition.West -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            cards()
            seatName(Modifier.rotate(90f))
        }
    }
}

/**
 * Filled rounded-rectangle action button used by the game table controls.
 * Mirrors the iOS `action(...)` helper so both platforms have the same look:
 *  - solid fill (no Material outline stroke)
 *  - rounded corners (8.dp)
 *  - single-line label with auto-shrink to fit narrow widths
 *  - three style variants: primary (filled green), secondary (white-12% fill),
 *    and accented (white-12% fill + custom text color, e.g. gold for "Cabo!").
 *
 * Designed to be placed inside a [Row] — it fills equal width via `weight(1f)`.
 */
@Composable
private fun RowScope.ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    accentColor: Color? = null,
    height: Dp = 40.dp,
    fontSize: TextUnit = 12.sp
) {
    val accentGreen = Color(0xFF2EBF8C)
    val bgDark = Color(0xFF101A14)
    val baseFill = if (primary) (accentColor ?: accentGreen) else Color.White.copy(alpha = 0.12f)
    val fillColor = if (enabled) baseFill else baseFill.copy(alpha = baseFill.alpha * 0.5f)
    val rawTextColor = when {
        primary -> bgDark
        accentColor != null -> accentColor
        else -> Color.White
    }
    val textColor = if (enabled) rawTextColor else rawTextColor.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(fillColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Auto-shrink the label to fit a single line within narrow buttons
        // (iOS does this implicitly via Text auto-sizing). We start at the
        // requested font size and step down a couple of times if measurement
        // says it would overflow, so labels like "Draw Discard" never wrap.
        var resolvedSize by remember(label, fontSize) { mutableStateOf(fontSize) }
        Text(
            text = label,
            color = textColor,
            fontSize = resolvedSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && resolvedSize.value > 9f) {
                    resolvedSize = (resolvedSize.value - 1f).sp
                }
            },
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    StatusChip(text = text, color = color, modifier = Modifier)
}

@Composable
private fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.30f)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableCard(
    text: String,
    isFaceUp: Boolean,
    isSelected: Boolean,
    kingSwapOutline: Boolean = false,
    isEmpty: Boolean,
    width: Dp = 60.dp,
    height: Dp = 84.dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    faceSymbolRotationDegrees: Float = 0f,
) {
    val borderColor = when {
        kingSwapOutline -> Color(0xFFE53935)
        isSelected -> Color.Yellow.copy(alpha = 0.9f)
        else -> Color.White.copy(alpha = 0.2f)
    }
    val borderWidth = when {
        kingSwapOutline -> 2.8.dp
        isSelected -> 2.4.dp
        else -> 1.2.dp
    }
    val bgColor = when {
        isEmpty -> Color.White.copy(alpha = 0.05f)
        isFaceUp -> Color.White
        else -> CardBack
    }

    val interactionSource = remember { MutableInteractionSource() }
    val gestureModifier = if (onTap != null || onDoubleTap != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onTap?.invoke() },
            onDoubleClick = onDoubleTap?.let { { it() } }
        )
    } else Modifier

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .then(gestureModifier)
            .then(
                if (isEmpty) Modifier.drawBehind {
                    val stroke = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = stroke,
                        topLeft = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 5.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            this.size.width - 10.dp.toPx(),
                            this.size.height - 10.dp.toPx()
                        )
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            if (isFaceUp) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.rotate(faceSymbolRotationDegrees)
                ) {
                    Text(
                        text = cardRankText(text),
                        style = TextStyle(
                            fontSize = when {
                                height <= 34.dp -> 10.sp
                                width > 64.dp -> 18.sp
                                width > 40.dp -> 14.sp
                                else -> 11.sp
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = cardTextColor(text)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = cardSuitText(text),
                        style = TextStyle(
                            fontSize = when {
                                height <= 34.dp -> 10.sp
                                width > 64.dp -> 18.sp
                                width > 40.dp -> 14.sp
                                else -> 11.sp
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = cardTextColor(text)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun cardTextColor(text: String): Color =
    if (text.contains("♥") || text.contains("♦")) Color.Red else Color.Black

private fun cardSuitText(text: String): String {
    val last = text.lastOrNull() ?: return ""
    return if ("♥♦♣♠".contains(last)) last.toString() else ""
}

private fun cardRankText(text: String): String {
    val suit = cardSuitText(text)
    return if (suit.isEmpty()) text else text.dropLast(suit.length)
}
