package com.navitech.cabo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navitech.cabo.ui.theme.BoardTheme
import com.navitech.cabo.ui.theme.CardDeckStyle
import com.navitech.cabo.ui.theme.TableAppearanceStore
import com.navitech.cabo.ui.theme.drawCardBackFillAndPattern
import com.navitech.cabo.ui.theme.faceBackground
import com.navitech.cabo.ui.theme.palette
import com.navitech.cabo.ui.theme.previewLabel
import com.navitech.cabo.ui.theme.suitColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val board by TableAppearanceStore.boardTheme.collectAsStateWithLifecycle()
    val deck by TableAppearanceStore.cardDeck.collectAsStateWithLifecycle()
    val palette = board.palette()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Table look", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { inset ->
        Column(
            modifier = Modifier
                .padding(inset)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.gradientTop, palette.gradientBottom),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Board colors",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "Each player chooses their own table on this device. It doesn’t sync to others.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Column(Modifier.selectableGroup()) {
                BoardTheme.entries.forEach { option ->
                    val selected = option == board
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = selected,
                                onClick = { TableAppearanceStore.setBoardTheme(option) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(option.previewLabel(), color = Color.White, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Card deck",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "Face colors and card-back patterns are decorative only.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Column(Modifier.selectableGroup()) {
                CardDeckStyle.entries.forEach { option ->
                    val selected = option == deck
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = selected,
                                onClick = { TableAppearanceStore.setCardDeck(option) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(option.previewLabel(), color = Color.White, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeckPreviewFace(deck = deck)
                DeckPreviewBack(deck = deck)
            }
        }
    }
}

@Composable
private fun DeckPreviewFace(deck: CardDeckStyle) {
    Box(
        modifier = Modifier
            .size(44.dp, 62.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color = deck.faceBackground())
            .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "A♥",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = deck.suitColor("A♥"),
        )
    }
}

@Composable
private fun DeckPreviewBack(deck: CardDeckStyle) {
    val density = LocalDensity.current
    val corner = 6.dp
    val cornerPx = with(density) { corner.toPx() }
    Box(
        modifier = Modifier
            .size(44.dp, 62.dp)
            .clip(RoundedCornerShape(corner))
            .drawBehind {
                drawCardBackFillAndPattern(deck, cornerPx)
            }
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(corner)),
    )
}
