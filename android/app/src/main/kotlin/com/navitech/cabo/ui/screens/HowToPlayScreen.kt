package com.navitech.cabo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navitech.cabo.models.Card
import com.navitech.cabo.models.Rank
import com.navitech.cabo.models.Suit
import com.navitech.cabo.ui.theme.CardBack
import com.navitech.cabo.ui.theme.HowToPlayGreenDark
import com.navitech.cabo.ui.theme.HowToPlayGreenDeep

private val CaptionAlpha = 0.72f
private val SurfaceAlpha = 0.10f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(HowToPlayGreenDark, HowToPlayGreenDeep)))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("How To Play", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Quick visual guide — same rules as around the table.",
                    color = Color.White.copy(alpha = CaptionAlpha),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                GuideSection(title = "Goal", caption = "Lowest total wins the round. Each card scores its rank as points.") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        ScoringChip(Card(Suit.SPADES, Rank.ACE), points = 1)
                        ScoringChip(Card(Suit.HEARTS, Rank.EIGHT), points = 8)
                        ScoringChip(Card(Suit.CLUBS, Rank.KING), points = 13)
                    }
                }

                GuideSection(
                    title = "Setup",
                    caption = "Everyone gets 4 face-down cards. Memorize any two of yours during the peek timer."
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { idx ->
                            DemoFaceDownCard(
                                modifier = Modifier.size(width = 34.dp, height = 48.dp),
                                emphasized = idx == 1 || idx == 3
                            )
                        }
                    }
                    Text(
                        "Gold outline = example peek picks",
                        color = Color.White.copy(alpha = CaptionAlpha),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                GuideSection(
                    title = "Your turn",
                    caption = "Draw from the deck or the discard pile. Then either swap into your hand or discard to use the drawn card’s power."
                ) {
                    FlowRowIllustration()
                    Text(
                        "Replacing: put the drawn card in a slot and discard what was there.\n" +
                            "Power play: discard the drawn card — if it’s J/Q/K you resolve that effect next.",
                        color = Color.White.copy(alpha = CaptionAlpha),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                GuideSection(title = "Face-card powers", caption = "Only when you discard that rank from your draw step.") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PowerCardDemo(Card(Suit.CLUBS, Rank.JACK), "Peek one of your cards.")
                        PowerCardDemo(Card(Suit.HEARTS, Rank.QUEEN), "Peek one opponent card.")
                        PowerCardDemo(Card(Suit.SPADES, Rank.KING), "Swap one of yours with any opponent card.")
                    }
                }

                GuideSection(
                    title = "Match the discard",
                    caption = "Anytime on your turn you may try to match the top discard’s rank with one of your face-up slots."
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Top discard", color = Color.White.copy(alpha = CaptionAlpha), fontSize = 12.sp)
                            DemoPlayingCard(Card(Suit.SPADES, Rank.FIVE), Modifier.size(width = 38.dp, height = 54.dp))
                        }
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                DemoPlayingCard(
                                    Card(Suit.DIAMONDS, Rank.FIVE),
                                    Modifier
                                        .size(width = 38.dp, height = 54.dp)
                                        .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                )
                                Text("✓ Match — remove yours", color = Color(0xFF81C784), fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                DemoPlayingCard(
                                    Card(Suit.DIAMONDS, Rank.THREE),
                                    Modifier
                                        .size(width = 38.dp, height = 54.dp)
                                        .border(2.dp, Color(0xFFE53935), RoundedCornerShape(6.dp))
                                )
                                Text("✗ Wrong — skip next turn", color = Color(0xFFFF8A80), fontSize = 11.sp)
                            }
                        }
                    }
                }

                GuideSection(
                    title = "Call Cabo",
                    caption = "When you think your total is lowest, call Cabo before drawing. Everyone gets one more lap; then scores are compared."
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Final turns", color = Color(0xFFF2BF4D), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text("Reveal & score", color = Color.White.copy(alpha = CaptionAlpha), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, caption: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = SurfaceAlpha),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(caption, color = Color.White.copy(alpha = CaptionAlpha), fontSize = 14.sp, lineHeight = 20.sp)
            content()
        }
    }
}

@Composable
private fun ScoringChip(card: Card, points: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DemoPlayingCard(card, Modifier.size(width = 38.dp, height = 54.dp))
        Text("$points pts", color = Color.White.copy(alpha = CaptionAlpha), fontSize = 11.sp)
    }
}

@Composable
private fun DemoFaceDownCard(modifier: Modifier = Modifier, emphasized: Boolean) {
    Box(
        modifier = modifier
            .background(CardBack, RoundedCornerShape(6.dp))
            .border(
                width = if (emphasized) 2.dp else 1.dp,
                color = if (emphasized) Color(0xFFF2BF4D) else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun DemoPlayingCard(card: Card, modifier: Modifier = Modifier) {
    val red = card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = card.shortName,
            color = if (red) Color(0xFFD32F2F) else Color(0xFF212121),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun PowerCardDemo(card: Card, blurb: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 112.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DemoPlayingCard(card, Modifier.size(width = 36.dp, height = 50.dp))
        Text(
            blurb,
            color = Color.White.copy(alpha = CaptionAlpha),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun FlowRowIllustration() {
    val arrowTint = Color.White.copy(alpha = 0.45f)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                DemoPileChip("Deck")
                Spacer(Modifier.height(4.dp))
                DemoFaceDownCard(Modifier.size(36.dp, 48.dp), emphasized = false)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = arrowTint)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                DemoPileChip("Discard")
                Spacer(Modifier.height(4.dp))
                DemoPlayingCard(Card(Suit.HEARTS, Rank.NINE), Modifier.size(36.dp, 48.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = arrowTint)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your hand", color = Color.White.copy(alpha = CaptionAlpha), fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { idx ->
                        if (idx == 2) {
                            DemoPlayingCard(Card(Suit.CLUBS, Rank.TWO), Modifier.size(30.dp, 42.dp))
                        } else {
                            DemoFaceDownCard(Modifier.size(30.dp, 42.dp), emphasized = false)
                        }
                    }
                }
                Text("example slot you're remembering", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DemoPileChip(label: String) {
    Text(label, color = Color.White.copy(alpha = CaptionAlpha), fontSize = 11.sp, fontWeight = FontWeight.Medium)
}
