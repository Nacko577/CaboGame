package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.ui.theme.HowToPlayGreenDark
import com.alex.cabogame.ui.theme.HowToPlayGreenDeep

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RuleCard("Goal", "End rounds with the lowest card total.")
                RuleCard("Start", "Each player gets 4 cards and peeks at 2.")
                RuleCard("Turn", "Draw from deck or discard, then replace a card or discard for power.")
                RuleCard("Powers", "J: peek your card, Q: peek opponent card, K: swap with opponent.")
                RuleCard("Match Discard", "Anytime, match top discard rank with one of your cards to remove it. Wrong guess means skip next turn.")
                RuleCard("Cabo", "Calling Cabo gives everyone one final turn until play returns to caller.")
            }
        }
    }
}

@Composable
private fun RuleCard(title: String, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text, color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
        }
    }
}
