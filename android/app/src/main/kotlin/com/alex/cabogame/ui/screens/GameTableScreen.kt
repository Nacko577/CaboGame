package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.models.*
import com.alex.cabogame.ui.theme.CardBack
import com.alex.cabogame.ui.theme.GreenDark
import com.alex.cabogame.ui.theme.GreenDeep
import com.alex.cabogame.viewmodel.GameViewModel

@Composable
fun GameTableScreen(viewModel: GameViewModel, onLeaveGame: () -> Unit) {
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
        Rank.KING -> "K: swap one of your cards with any opponents card"
        Rank.JACK -> "J: peek at one of your cards"
        Rank.QUEEN -> "Q: peek at one of any opponents card"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(GreenDark, GreenDeep)))
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLeaveGame) {
                    Text("Leave", color = Color.White)
                }
            }

            // Header HUD
            if (gameState.phase == TurnPhase.INITIAL_PEEK) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val headerText = if (isMyTurn) "Your Turn" else "${currentTurnName}'s turn"
                    val headerColor = if (isMyTurn) Color(0xFF4CAF50) else Color(0xFFF2BF4D)
                    StatusChip(headerText, color = headerColor)
                    localPlayer?.let { me ->
                        if (me.roundsToSkip > 0) StatusChip("Skip ${me.roundsToSkip}", color = Color(0xFFF44336))
                    }
                }
            }

            // Updated iOS-style board layout: N/S/E/W seats around a center table.
            val north = opponents.getOrNull(0)
            val east = opponents.getOrNull(1)
            val west = opponents.getOrNull(2)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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

                    // North seat
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        player = north,
                        isLocal = false,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )

                    // South seat (local)
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        player = localPlayer,
                        isLocal = true,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )

                    // East seat
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp),
                        player = east,
                        isLocal = false,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )

                    // West seat
                    SeatRow(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp),
                        player = west,
                        isLocal = false,
                        viewModel = viewModel,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectOwn = { selectedOwnIndex = it },
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx }
                    )
                }
            }

            // Action controls
            if (gameState.winnerID == null) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        if (gameState.phase == TurnPhase.INITIAL_PEEK) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Double-tap your cards to peek.", color = Color.White)
                                val remaining = maxOf(0, 2 - viewModel.initialPeekedOwnIndices.size)
                                Text("Peeks left: $remaining", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.attemptMatchDiscard(selectedOwnIndex) },
                            enabled = gameState.phase != TurnPhase.INITIAL_PEEK && gameState.winnerID == null && localPlayer != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Match Discard (Anytime)") }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.draw(DrawSource.DECK) },
                                enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_DRAW,
                                modifier = Modifier.weight(1f)
                            ) { Text("Draw Deck") }
                            OutlinedButton(
                                onClick = { viewModel.draw(DrawSource.DISCARD_TOP) },
                                enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_DRAW,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) { Text("Draw Discard") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.replaceWithDrawnCard(selectedOwnIndex) },
                                enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD,
                                modifier = Modifier.weight(1f)
                            ) { Text("Replace Selected") }
                            OutlinedButton(
                                onClick = { viewModel.discardForEffect() },
                                enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) { Text("Discard for Effect") }
                        }

                        if (gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION && isMyTurn && activeSpecialRank != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(specialPowerText ?: "", color = Color.White, fontWeight = FontWeight.SemiBold)
                                val mySelectedCard = localPlayer?.hand?.getOrNull(selectedOwnIndex)
                                val selectedOpponentPlayer = selectedOpponentID?.let { id ->
                                    gameState.players.firstOrNull { it.id == id }
                                }
                                val opponentSelectedCard = selectedOpponentPlayer?.hand?.getOrNull(selectedOpponentIndex)
                                when (activeSpecialRank) {
                                    Rank.JACK -> {
                                        Text("Pick one of your cards above, then use Jack.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Button(
                                            onClick = {
                                                localPlayer?.let { me ->
                                                    viewModel.resolveSpecial(SpecialAction.LookOwnCard(me.id, selectedOwnIndex))
                                                }
                                            },
                                            enabled = mySelectedCard != null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Peek") }
                                    }
                                    Rank.QUEEN -> {
                                        Text("Tap an opponent card above, then use Queen.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Button(
                                            onClick = {
                                                selectedOpponentID?.let { targetID ->
                                                    viewModel.resolveSpecial(SpecialAction.LookOtherCard(targetID, selectedOpponentIndex))
                                                }
                                            },
                                            enabled = opponentSelectedCard != null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Peek") }
                                    }
                                    Rank.KING -> {
                                        Text("Pick your card and an opponent card above, then use King.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Button(
                                            onClick = {
                                                val targetID = selectedOpponentID ?: return@Button
                                                localPlayer?.let { me ->
                                                    viewModel.resolveSpecial(
                                                        SpecialAction.SwapCards(me.id, selectedOwnIndex, targetID, selectedOpponentIndex)
                                                    )
                                                }
                                            },
                                            enabled = mySelectedCard != null && opponentSelectedCard != null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Swap") }
                                    }
                                    else -> Unit
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.callCabo() },
                            enabled = isMyTurn && gameState.phase != TurnPhase.INITIAL_PEEK && gameState.caboCallerID == null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Call Cabo") }
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

            // Status chips
            when {
                gameState.winnerID != null -> {
                    val winner = gameState.players.firstOrNull { it.id == gameState.winnerID }
                    if (winner != null) StatusChip("Winner: ${winner.name} (${winner.score()} points)", color = Color(0xFF26C6DA))
                }
                gameState.caboCallerID != null -> {
                    val caller = gameState.players.firstOrNull { it.id == gameState.caboCallerID }
                    if (caller != null) StatusChip("Cabo called by ${caller.name}. Final turns in progress...", color = Color(0xFFFF9800))
                }
                viewModel.matchDiscardStatusText != null -> {
                    StatusChip(viewModel.matchDiscardStatusText!!, color = Color(0xFFFF9800))
                }
                gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION && specialPowerText != null -> {
                    StatusChip(specialPowerText, color = Color(0xFF2EBF8C))
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun SeatRow(
    modifier: Modifier = Modifier,
    player: Player?,
    isLocal: Boolean,
    viewModel: GameViewModel,
    selectedOwnIndex: Int,
    onSelectOwn: (Int) -> Unit,
    selectedOpponentID: String?,
    selectedOpponentIndex: Int,
    onSelectOpponent: (String, Int) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (player == null) {
            Text("Empty", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
        } else {
            val isTurn = viewModel.gameState.currentPlayerID == player.id
            Text(
                if (isLocal) "You" else player.name,
                color = if (isLocal) Color(0xFFF2BF4D) else if (isTurn) Color(0xFF4CAF50) else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                player.hand.take(4).forEachIndexed { idx, card ->
                    val revealedBase = if (isLocal) {
                        viewModel.gameState.phase == TurnPhase.INITIAL_PEEK && viewModel.initialPeekedOwnIndices.contains(idx) ||
                            viewModel.jackPeekedOwnIndex == idx
                    } else {
                        viewModel.queenPeekedOpponentPlayerID == player.id && viewModel.queenPeekedOpponentIndex == idx
                    }
                    val revealed = revealedBase || viewModel.gameState.winnerID != null
                    TableCard(
                        text = if (revealed && card != null) card.shortName else "",
                        isFaceUp = revealed && card != null,
                        isSelected = if (isLocal) selectedOwnIndex == idx else (selectedOpponentID == player.id && selectedOpponentIndex == idx),
                        isEmpty = card == null,
                        width = 32.dp,
                        height = 46.dp,
                        modifier = Modifier.pointerInput(player.id, idx) {
                            detectTapGestures(
                                onTap = {
                                    if (isLocal) onSelectOwn(idx) else onSelectOpponent(player.id, idx)
                                },
                                onDoubleTap = {
                                    if (isLocal) viewModel.peekInitialCard(idx)
                                }
                            )
                        }
                    )
                }
            }
        }
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

@Composable
private fun TableCard(
    text: String,
    isFaceUp: Boolean,
    isSelected: Boolean,
    isEmpty: Boolean,
    width: Dp = 60.dp,
    height: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color.Yellow.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.2f)
    val borderWidth = if (isSelected) 2.4.dp else 1.2.dp
    val bgColor = when {
        isEmpty -> Color.White.copy(alpha = 0.05f)
        isFaceUp -> Color.White
        else -> CardBack
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
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
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cardRankText(text),
                        style = TextStyle(
                            fontSize = if (width > 64.dp) 18.sp else if (width > 40.dp) 14.sp else 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cardTextColor(text)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = cardSuitText(text),
                        style = TextStyle(
                            fontSize = if (width > 64.dp) 18.sp else if (width > 40.dp) 14.sp else 11.sp,
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
