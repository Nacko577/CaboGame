package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.ui.theme.GreenDark
import com.alex.cabogame.ui.theme.GreenDeep
import com.alex.cabogame.viewmodel.GameViewModel

@Composable
fun LobbyScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(GreenDark, GreenDeep)))
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text("Back") }

            Panel {
                Text("Player", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = viewModel.playerName,
                    onValueChange = { viewModel.playerName = it },
                    placeholder = { Text("Your name", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.12f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Panel {
                Text("Host", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = { viewModel.hostLobby() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Host Game")
                }
                if (viewModel.hostedCode != null) {
                    Text(
                        "Join code: ${viewModel.hostedCode}",
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Panel {
                Text("Join", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = viewModel.joinCodeInput,
                    onValueChange = { viewModel.joinCodeInput = it.uppercase() },
                    placeholder = { Text("Code", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.12f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { viewModel.joinLobby() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Join by Code") }
            }

            Panel {
                Text("Lobby", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(viewModel.statusText, color = Color.White.copy(alpha = 0.85f))
                if (viewModel.peers.isEmpty()) {
                    Text("No peers connected", color = Color.White.copy(alpha = 0.65f))
                } else {
                    viewModel.peers.forEach { peer ->
                        Text("• ${peer.displayName}", color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            if (viewModel.hostedCode != null) {
                Panel {
                    Text("Game Setup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val playerCount = viewModel.gameState.players.size
                    Text("Players: $playerCount", color = Color.White.copy(alpha = 0.85f))
                    Button(
                        onClick = { viewModel.startGameAsHost() },
                        enabled = playerCount >= 2,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start Game") }
                }
            }

            if (viewModel.lastError != null) {
                Panel {
                    Text("Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(viewModel.lastError ?: "", color = Color.Red)
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
