package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.models.*
import com.alex.cabogame.viewmodel.GameViewModel

// Colors
private val BgDark = Color(0xFF0D1A14)
private val BgTable = Color(0xFF142420)
private val TableBorder = Color(0xFF2EAA73)
private val AccentGreen = Color(0xFF2EBF8E)
private val AccentGold = Color(0xFFF2C04D)
private val CardBackColor = Color(0xFF1E2E38)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)

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
    val currentTurnName = gameState.players.firstOrNull { it.id == gameState.currentPlayerID }?.name ?: "..."
    val activeSpecialRank = if (gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION)
        gameState.discardPile.lastOrNull()?.rank?.takeIf { it == Rank.JACK || it == Rank.QUEEN || it == Rank.KING }
    else null

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 750
    val cardW = if (isCompact) 34.dp else 40.dp
    val cardH = cardW * 1.4f
    val myCardW = if (isCompact) 46.dp else 54.dp
    val myCardH = myCardW * 1.4f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            GameHeader(
                isMyTurn = isMyTurn,
                currentTurnName = currentTurnName,
                skipCount = localPlayer?.roundsToSkip ?: 0,
                isCompact = isCompact
            )

            Spacer(Modifier.height(6.dp))

            // Rectangular Table
            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgTable)
                    .border(2.dp, TableBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top opponents (indices 0, 1, 2)
                    TopOpponentsRow(
                        opponents = opponents.take(3),
                        viewModel = viewModel,
                        selectedOpponentID = selectedOpponentID,
                        selectedOpponentIndex = selectedOpponentIndex,
                        onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx },
                        cardW = cardW,
                        cardH = cardH,
                        gameState = gameState
                    )

                    // Middle row: side opponents + center piles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left opponent (index 3)
                        SideOpponent(
                            player = opponents.getOrNull(3),
                            opponentIndex = 3,
                            viewModel = viewModel,
                            selectedOpponentID = selectedOpponentID,
                            selectedOpponentIndex = selectedOpponentIndex,
                            onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx },
                            cardW = cardW,
                            cardH = cardH,
                            gameState = gameState,
                            modifier = Modifier.weight(0.22f)
                        )

                        // Center piles
                        CenterPiles(
                            gameState = gameState,
                            viewModel = viewModel,
                            isMyTurn = isMyTurn,
                            isCompact = isCompact,
                            modifier = Modifier.weight(0.56f)
                        )

                        // Right opponent (index 4)
                        SideOpponent(
                            player = opponents.getOrNull(4),
                            opponentIndex = 4,
                            viewModel = viewModel,
                            selectedOpponentID = selectedOpponentID,
                            selectedOpponentIndex = selectedOpponentIndex,
                            onSelectOpponent = { id, idx -> selectedOpponentID = id; selectedOpponentIndex = idx },
                            cardW = cardW,
                            cardH = cardH,
                            gameState = gameState,
                            modifier = Modifier.weight(0.22f)
                        )
                    }

                    // My hand at bottom
                    MyHand(
                        player = localPlayer,
                        isMyTurn = isMyTurn,
                        selectedOwnIndex = selectedOwnIndex,
                        onSelectCard = { selectedOwnIndex = it },
                        viewModel = viewModel,
                        gameState = gameState,
                        cardW = myCardW,
                        cardH = myCardH,
                        isCompact = isCompact
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Status banner
            StatusBanner(
                gameState = gameState,
                viewModel = viewModel,
                activeSpecialRank = activeSpecialRank
            )

            Spacer(Modifier.height(6.dp))

            // Action buttons
            ActionButtons(
                gameState = gameState,
                viewModel = viewModel,
                localPlayer = localPlayer,
                isMyTurn = isMyTurn,
                selectedOwnIndex = selectedOwnIndex,
                selectedOpponentID = selectedOpponentID,
                selectedOpponentIndex = selectedOpponentIndex,
                activeSpecialRank = activeSpecialRank,
                isCompact = isCompact
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GameHeader(
    isMyTurn: Boolean,
    currentTurnName: String,
    skipCount: Int,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Turn indicator
        Row(
            modifier = Modifier
                .background(
                    (if (isMyTurn) AccentGreen else AccentGold).copy(alpha = 0.2f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isMyTurn) AccentGreen else AccentGold, CircleShape)
            )
            Text(
                text = if (isMyTurn) "Your Turn" else currentTurnName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Cabo",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (skipCount > 0) {
            Text(
                text = "Skip $skipCount",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(Color.Red.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        } else {
            Spacer(Modifier.width(60.dp))
        }
    }
}

@Composable
private fun TopOpponentsRow(
    opponents: List<Player>,
    viewModel: GameViewModel,
    selectedOpponentID: String?,
    selectedOpponentIndex: Int,
    onSelectOpponent: (String, Int) -> Unit,
    cardW: Dp,
    cardH: Dp,
    gameState: GameState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        opponents.forEachIndexed { idx, player ->
            OpponentSlot(
                player = player,
                viewModel = viewModel,
                isSelected = selectedOpponentID == player.id,
                selectedCardIndex = if (selectedOpponentID == player.id) selectedOpponentIndex else -1,
                onSelectCard = { cardIdx -> onSelectOpponent(player.id, cardIdx) },
                cardW = cardW,
                cardH = cardH,
                gameState = gameState,
                vertical = false
            )
        }
        // Empty slots
        repeat(3 - opponents.size) {
            EmptyPlayerSlot(cardW = cardW, cardH = cardH, vertical = false)
        }
    }
}

@Composable
private fun SideOpponent(
    player: Player?,
    opponentIndex: Int,
    viewModel: GameViewModel,
    selectedOpponentID: String?,
    selectedOpponentIndex: Int,
    onSelectOpponent: (String, Int) -> Unit,
    cardW: Dp,
    cardH: Dp,
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (player != null) {
            OpponentSlot(
                player = player,
                viewModel = viewModel,
                isSelected = selectedOpponentID == player.id,
                selectedCardIndex = if (selectedOpponentID == player.id) selectedOpponentIndex else -1,
                onSelectCard = { cardIdx -> onSelectOpponent(player.id, cardIdx) },
                cardW = cardW,
                cardH = cardH,
                gameState = gameState,
                vertical = true
            )
        } else {
            EmptyPlayerSlot(cardW = cardW, cardH = cardH, vertical = true)
        }
    }
}

@Composable
private fun OpponentSlot(
    player: Player,
    viewModel: GameViewModel,
    isSelected: Boolean,
    selectedCardIndex: Int,
    onSelectCard: (Int) -> Unit,
    cardW: Dp,
    cardH: Dp,
    gameState: GameState,
    vertical: Boolean
) {
    val isTurn = gameState.currentPlayerID == player.id

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                when {
                    isTurn -> AccentGreen.copy(alpha = 0.15f)
                    isSelected -> AccentGold.copy(alpha = 0.1f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(8.dp)
            )
            .padding(5.dp)
    ) {
        Text(
            text = player.name,
            color = if (isTurn) AccentGreen else TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        if (vertical) {
            // 2x2 grid
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    player.hand.take(2).forEachIndexed { idx, card ->
                        OpponentCard(
                            card = card,
                            index = idx,
                            player = player,
                            viewModel = viewModel,
                            isSelected = selectedCardIndex == idx,
                            onSelect = { onSelectCard(idx) },
                            cardW = cardW,
                            cardH = cardH,
                            gameState = gameState
                        )
                    }
                }
                if (player.hand.size > 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        player.hand.drop(2).forEachIndexed { idx, card ->
                            OpponentCard(
                                card = card,
                                index = idx + 2,
                                player = player,
                                viewModel = viewModel,
                                isSelected = selectedCardIndex == idx + 2,
                                onSelect = { onSelectCard(idx + 2) },
                                cardW = cardW,
                                cardH = cardH,
                                gameState = gameState
                            )
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                player.hand.forEachIndexed { idx, card ->
                    OpponentCard(
                        card = card,
                        index = idx,
                        player = player,
                        viewModel = viewModel,
                        isSelected = selectedCardIndex == idx,
                        onSelect = { onSelectCard(idx) },
                        cardW = cardW,
                        cardH = cardH,
                        gameState = gameState
                    )
                }
            }
        }
    }
}

@Composable
private fun OpponentCard(
    card: Card?,
    index: Int,
    player: Player,
    viewModel: GameViewModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    cardW: Dp,
    cardH: Dp,
    gameState: GameState
) {
    val isRevealed = (viewModel.queenPeekedOpponentPlayerID == player.id && viewModel.queenPeekedOpponentIndex == index) ||
            gameState.winnerID != null

    MiniCard(
        text = if (card != null && isRevealed) card.shortName else "",
        isFaceUp = isRevealed && card != null,
        isEmpty = card == null,
        isSelected = isSelected,
        width = cardW,
        height = cardH,
        modifier = Modifier.clickable { onSelect() }
    )
}

@Composable
private fun EmptyPlayerSlot(cardW: Dp, cardH: Dp, vertical: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(5.dp)
            .alpha(0.35f)
    ) {
        Text(
            text = "Empty",
            color = TextSecondary.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        if (vertical) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) { EmptyCard(cardW, cardH) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) { EmptyCard(cardW, cardH) }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(4) { EmptyCard(cardW, cardH) }
            }
        }
    }
}

@Composable
private fun CenterPiles(
    gameState: GameState,
    viewModel: GameViewModel,
    isMyTurn: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val pileW = if (isCompact) 42.dp else 50.dp
    val pileH = pileW * 1.4f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 16.dp else 24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Discard
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Discard", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            val top = gameState.discardPile.lastOrNull()
            PileCard(
                text = top?.shortName ?: "",
                isFaceUp = top != null,
                isEmpty = top == null,
                highlight = false,
                width = pileW,
                height = pileH
            )
        }

        // Drawn / Deck
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val hasPending = isMyTurn && gameState.pendingDraw != null
            Text(
                text = if (hasPending) "Drawn" else "Deck",
                color = if (hasPending) AccentGold else TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            if (hasPending) {
                PileCard(
                    text = gameState.pendingDraw!!.card.shortName,
                    isFaceUp = true,
                    isEmpty = false,
                    highlight = true,
                    width = pileW,
                    height = pileH
                )
            } else {
                PileCard(
                    text = "",
                    isFaceUp = false,
                    isEmpty = false,
                    highlight = false,
                    width = pileW,
                    height = pileH
                )
            }
        }
    }
}

@Composable
private fun MyHand(
    player: Player?,
    isMyTurn: Boolean,
    selectedOwnIndex: Int,
    onSelectCard: (Int) -> Unit,
    viewModel: GameViewModel,
    gameState: GameState,
    cardW: Dp,
    cardH: Dp,
    isCompact: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isMyTurn) AccentGold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(
            text = "You",
            color = if (isMyTurn) AccentGold else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        player?.let { me ->
            Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)) {
                me.hand.forEachIndexed { idx, card ->
                    val isRevealed = when {
                        gameState.winnerID != null -> true
                        gameState.phase == TurnPhase.INITIAL_PEEK && viewModel.initialPeekedOwnIndices.contains(idx) -> true
                        viewModel.jackPeekedOwnIndex == idx -> true
                        else -> false
                    }
                    MiniCard(
                        text = if (card != null && isRevealed) card.shortName else "",
                        isFaceUp = isRevealed && card != null,
                        isEmpty = card == null,
                        isSelected = idx == selectedOwnIndex,
                        width = cardW,
                        height = cardH,
                        modifier = Modifier.clickable { onSelectCard(idx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    gameState: GameState,
    viewModel: GameViewModel,
    activeSpecialRank: Rank?
) {
    val text: String?
    val color: Color

    when {
        gameState.winnerID != null -> {
            val winner = gameState.players.firstOrNull { it.id == gameState.winnerID }
            text = winner?.let { "Winner: ${it.name} (${it.score()} pts)" }
            color = Color(0xFF26C6DA)
        }
        gameState.phase == TurnPhase.INITIAL_PEEK -> {
            val remaining = maxOf(0, 2 - gameState.currentPlayerPeekedIndices.size)
            text = "Peek 2 cards ($remaining left)"
            color = AccentGold
        }
        gameState.caboCallerID != null -> {
            val caller = gameState.players.firstOrNull { it.id == gameState.caboCallerID }
            text = caller?.let { "Cabo! ${it.name} - Final turns" }
            color = Color(0xFFFF9800)
        }
        viewModel.matchDiscardStatusText != null -> {
            text = viewModel.matchDiscardStatusText
            color = Color(0xFFFF9800)
        }
        gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION && activeSpecialRank != null -> {
            text = "Resolve ${activeSpecialRank.display} power"
            color = AccentGreen
        }
        viewModel.initialPeekSecondsRemaining != null && gameState.playersFinishedInitialPeek >= gameState.players.size -> {
            text = "Starting in ${viewModel.initialPeekSecondsRemaining}s"
            color = Color(0xFFFFEB3B)
        }
        else -> {
            text = null
            color = Color.Transparent
        }
    }

    if (text != null) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ActionButtons(
    gameState: GameState,
    viewModel: GameViewModel,
    localPlayer: Player?,
    isMyTurn: Boolean,
    selectedOwnIndex: Int,
    selectedOpponentID: String?,
    selectedOpponentIndex: Int,
    activeSpecialRank: Rank?,
    isCompact: Boolean
) {
    val btnH = if (isCompact) 36.dp else 40.dp
    val fontSize = if (isCompact) 11.sp else 12.sp

    when {
        gameState.winnerID != null -> {
            // Rematch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel.isHostUser) {
                    ActionButton("New Game", true, btnH, fontSize, Modifier.weight(1f)) {
                        viewModel.startNewGameRound()
                    }
                } else {
                    ActionButton(
                        text = if (viewModel.isLocalPlayerReadyForNewGame) "Ready!" else "Ready",
                        primary = true,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = gameState.rematchRequestedByHost
                    ) {
                        viewModel.toggleReadyForNewGame()
                    }
                }
            }
        }
        gameState.phase == TurnPhase.INITIAL_PEEK -> {
            val remaining = maxOf(0, 2 - gameState.currentPlayerPeekedIndices.size)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ActionButton(
                    text = "Peek Card",
                    primary = true,
                    height = btnH,
                    fontSize = fontSize,
                    enabled = isMyTurn && remaining > 0 && gameState.playersFinishedInitialPeek < gameState.players.size
                ) {
                    viewModel.peekInitialCard(selectedOwnIndex)
                }
            }
        }
        gameState.phase == TurnPhase.WAITING_FOR_SPECIAL_RESOLUTION && isMyTurn && activeSpecialRank != null -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                when (activeSpecialRank) {
                    Rank.JACK -> ActionButton("Peek Own Card", true, btnH, fontSize) {
                        localPlayer?.let { me ->
                            viewModel.resolveSpecial(SpecialAction.LookOwnCard(me.id, selectedOwnIndex))
                        }
                    }
                    Rank.QUEEN -> ActionButton(
                        text = "Peek Opponent Card",
                        primary = true,
                        height = btnH,
                        fontSize = fontSize,
                        enabled = selectedOpponentID != null
                    ) {
                        selectedOpponentID?.let { targetID ->
                            viewModel.resolveSpecial(SpecialAction.LookOtherCard(targetID, selectedOpponentIndex))
                        }
                    }
                    Rank.KING -> ActionButton(
                        text = "Swap Cards",
                        primary = true,
                        height = btnH,
                        fontSize = fontSize,
                        enabled = selectedOpponentID != null
                    ) {
                        localPlayer?.let { me ->
                            selectedOpponentID?.let { targetID ->
                                viewModel.resolveSpecial(
                                    SpecialAction.SwapCards(me.id, selectedOwnIndex, targetID, selectedOpponentIndex)
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        else -> {
            // Normal game actions
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionButton(
                        text = "Draw Deck",
                        primary = true,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_DRAW
                    ) {
                        viewModel.draw(DrawSource.DECK)
                    }
                    ActionButton(
                        text = "Draw Discard",
                        primary = false,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_DRAW
                    ) {
                        viewModel.draw(DrawSource.DISCARD_TOP)
                    }
                    ActionButton(
                        text = "Match",
                        primary = false,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = gameState.phase != TurnPhase.INITIAL_PEEK && localPlayer != null
                    ) {
                        viewModel.attemptMatchDiscard(selectedOwnIndex)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionButton(
                        text = "Replace",
                        primary = true,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD
                    ) {
                        viewModel.replaceWithDrawnCard(selectedOwnIndex)
                    }
                    ActionButton(
                        text = "Discard",
                        primary = false,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = isMyTurn && gameState.phase == TurnPhase.WAITING_FOR_PLACEMENT_OR_DISCARD
                    ) {
                        viewModel.discardForEffect()
                    }
                    ActionButton(
                        text = "Cabo!",
                        primary = false,
                        height = btnH,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        accent = AccentGold,
                        enabled = isMyTurn && gameState.phase != TurnPhase.INITIAL_PEEK && gameState.caboCallerID == null
                    ) {
                        viewModel.callCabo()
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    primary: Boolean,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (primary) (accent ?: AccentGreen) else Color.White.copy(alpha = 0.12f)
    val textColor = if (primary) BgDark else TextPrimary

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniCard(
    text: String,
    isFaceUp: Boolean,
    isEmpty: Boolean,
    isSelected: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isEmpty -> Color.White.copy(alpha = 0.05f)
        isFaceUp -> Color.White
        else -> CardBackColor
    }
    val border = if (isSelected) AccentGold else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .border(
                width = if (isSelected) 2.dp else 0.8.dp,
                color = border,
                shape = RoundedCornerShape(5.dp)
            )
            .then(
                if (isEmpty) Modifier.drawBehind {
                    val stroke = Stroke(
                        width = 0.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = stroke,
                        topLeft = androidx.compose.ui.geometry.Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - 6.dp.toPx(),
                            size.height - 6.dp.toPx()
                        )
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            if (isFaceUp) {
                Text(
                    text = text,
                    fontSize = if (width > 44.dp) 14.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor(text)
                )
            } else {
                Text(
                    text = "♠",
                    fontSize = if (width > 44.dp) 14.sp else 10.sp,
                    color = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
private fun PileCard(
    text: String,
    isFaceUp: Boolean,
    isEmpty: Boolean,
    highlight: Boolean,
    width: Dp,
    height: Dp
) {
    val bg = when {
        isEmpty -> Color.White.copy(alpha = 0.08f)
        isFaceUp -> Color.White
        else -> CardBackColor
    }
    val border = if (highlight) AccentGold else Color.White.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(
                width = if (highlight) 2.5.dp else 1.dp,
                color = border,
                shape = RoundedCornerShape(7.dp)
            )
            .then(
                if (isEmpty) Modifier.drawBehind {
                    val stroke = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                        style = stroke,
                        topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - 8.dp.toPx(),
                            size.height - 8.dp.toPx()
                        )
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            if (isFaceUp) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor(text)
                )
            } else {
                Text(
                    text = "♣",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width, height)
            .drawBehind {
                val stroke = Stroke(
                    width = 0.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.15f),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = stroke
                )
            }
    )
}

@Composable
private fun Modifier.alpha(alpha: Float): Modifier = this.then(
    Modifier.drawBehind {
        drawRect(Color.Transparent.copy(alpha = 1f - alpha))
    }
)

private fun cardTextColor(text: String): Color =
    if (text.contains("♥") || text.contains("♦")) Color.Red else Color.Black
