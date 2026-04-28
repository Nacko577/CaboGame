package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.models.*
import com.alex.cabogame.ui.theme.CardBack
import com.alex.cabogame.ui.theme.GreenDark
import com.alex.cabogame.ui.theme.GreenDeep
import com.alex.cabogame.viewmodel.GameViewModel

@Composable
fun GameTableScreen(viewModel: GameViewModel) {
    var selectedOwnIndex by remember { mutableIntStateOf(0) }
    var selectedOpponentIndex by remember { mutableIntStateOf(0) }
    var selectedOpponentID by remember { mutableStateOf<String?>(null) }

    val gameState = viewModel.gameState
    val localPlayerID = viewModel.localPlayerID
    val localPlayer = gameState.players.firstOrNull { it.id == localPlayerID }
    val opponents = gameState.players.filter { it.id != localPlayerID }
    val isMyTurn = gameState.currentPlayerID == localPlayerID
    val currentTurnName = gameState.players.firstOrNull { it.id == gameState.currentPlayerID }?.name ?: "Unknown"
    val activeSpecialRank = if (gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION)
        gameState.discardPile.lastOrNull()?.rank?.takeIf { it == Rank.JACK || it == Rank.QUEEN || it == Rank.KING }
    else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(GreenDark, GreenDeep)))
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // Header HUD
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip(
                    "Turn: $currentTurnName",
                    color = if (isMyTurn) Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
                localPlayer?.let { me ->
                    if (me.roundsToSkip > 0) StatusChip("Skip ${me.roundsToSkip}", color = Color(0xFFF44336))
                }
            }

            // Center board
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Discard", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    val top = gameState.discardPile.lastOrNull()
                    TableCard(text = top?.shortName ?: "", isFaceUp = top != null, isSelected = false, isEmpty = top == null)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Drawn", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    val drawnText = when {
                        isMyTurn && gameState.pendingDraw != null -> gameState.pendingDraw.card.shortName
                        viewModel.lastReplacedIncomingCard != null -> viewModel.lastReplacedIncomingCard!!.shortName
                        else -> null
                    }
                    TableCard(text = drawnText ?: "", isFaceUp = drawnText != null, isSelected = drawnText != null && isMyTurn && gameState.pendingDraw != null, isEmpty = drawnText == null)
                }
                Spacer(Modifier.weight(1f))
                val attempted = viewModel.matchAttemptOwnCard
                val topDiscard = viewModel.matchAttemptTopDiscardCard
                if (attempted != null && topDiscard != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Match Attempt", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TableCard(topDiscard.shortName, isFaceUp = true, isSelected = false, isEmpty = false, width = 54.dp, height = 74.dp)
                            Text("vs", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            TableCard(attempted.shortName, isFaceUp = true, isSelected = false, isEmpty = false, width = 54.dp, height = 74.dp)
                        }
                    }
                }
            }

            // Opponents
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Opponents", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                opponents.forEach { opp ->
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text(opp.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("${opp.hand.filterNotNull().size} cards", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                opp.hand.indices.forEach { idx ->
                                    val card = opp.hand[idx]
                                    val revealed = viewModel.queenPeekedOpponentPlayerID == opp.id && viewModel.queenPeekedOpponentIndex == idx
                                    TableCard(
                                        text = if (revealed && card != null) card.shortName else if (card != null) "🂠" else "",
                                        isFaceUp = revealed && card != null,
                                        isSelected = selectedOpponentID == opp.id && selectedOpponentIndex == idx,
                                        isEmpty = card == null,
                                        width = 54.dp, height = 74.dp,
                                        modifier = Modifier.clickable { selectedOpponentID = opp.id; selectedOpponentIndex = idx }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Local hand
            localPlayer?.let { me ->
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Your Hand", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            me.hand.indices.forEach { idx ->
                                val card = me.hand[idx]
                                val revealed = when {
                                    gameState.winnerID != null -> true
                                    gameState.phase == TurnPhase.INITIAL_PEEK && viewModel.initialPeekedOwnIndices.contains(idx) -> true
                                    viewModel.jackPeekedOwnIndex == idx -> true
                                    else -> false
                                }
                                val displayText = if (card != null) (if (revealed) card.shortName else "🂠") else ""
                                TableCard(
                                    text = displayText,
                                    isFaceUp = revealed && card != null,
                                    isSelected = idx == selectedOwnIndex,
                                    isEmpty = card == null,
                                    width = 68.dp, height = 96.dp,
                                    modifier = Modifier.clickable { selectedOwnIndex = idx }
                                )
                            }
                        }
                    }
                }
            }

            // Action controls
            if (gameState.winnerID == null) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        if (gameState.phase == TurnPhase.INITIAL_PEEK) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Initial setup: peek 2 of your cards", color = Color.White)
                                val remaining = maxOf(0, 2 - gameState.currentPlayerPeekedIndices.size)
                                Text("Remaining peeks: $remaining", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Button(
                                    onClick = { viewModel.peekInitialCard(selectedOwnIndex) },
                                    enabled = isMyTurn && remaining > 0 && gameState.playersFinishedInitialPeek < gameState.players.size,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Peek Selected Card") }
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
                                Text("Resolve ${activeSpecialRank.display} power", color = Color.White)
                                when (activeSpecialRank) {
                                    Rank.JACK -> {
                                        Text("Pick one of your cards above, then use Jack.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Button(
                                            onClick = {
                                                localPlayer?.let { me ->
                                                    viewModel.resolveSpecial(SpecialAction.LookOwnCard(me.id, selectedOwnIndex))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Use Jack: Peek selected own card") }
                                    }
                                    Rank.QUEEN -> {
                                        Text("Tap an opponent card above, then use Queen.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Button(
                                            onClick = {
                                                selectedOpponentID?.let { targetID ->
                                                    viewModel.resolveSpecial(SpecialAction.LookOtherCard(targetID, selectedOpponentIndex))
                                                }
                                            },
                                            enabled = selectedOpponentID != null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Use Queen: Peek selected opponent card") }
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
                                            enabled = selectedOpponentID != null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Use King: Swap selected cards") }
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
            viewModel.matchDiscardStatusText?.let { StatusChip(it, color = Color(0xFFFF9800)) }

            val secs = viewModel.initialPeekSecondsRemaining
            if (secs != null && gameState.phase == TurnPhase.INITIAL_PEEK && gameState.playersFinishedInitialPeek >= gameState.players.size) {
                StatusChip("Game starts in ${secs}s", color = Color(0xFFFFEB3B))
            }

            gameState.winnerID?.let { winnerID ->
                val winner = gameState.players.firstOrNull { it.id == winnerID }
                if (winner != null) StatusChip("Winner: ${winner.name} (${winner.score()} points)", color = Color(0xFF26C6DA))
            } ?: gameState.caboCallerID?.let { callerID ->
                val caller = gameState.players.firstOrNull { it.id == callerID }
                if (caller != null) StatusChip("Cabo called by ${caller.name}. Final turns in progress...", color = Color(0xFFFF9800))
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.35f)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
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
            val displayText = if (isFaceUp) text else "🂠"
            val isLarge = width > 64.dp
            Text(
                text = displayText,
                style = TextStyle(
                    fontSize = if (isLarge) 18.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFaceUp) cardTextColor(text) else Color.White
                )
            )
        }
    }
}

private fun cardTextColor(text: String): Color =
    if (text.contains("♥") || text.contains("♦")) Color.Red else Color.Black
