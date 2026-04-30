package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.viewmodel.GameViewModel
import com.alex.cabogame.viewmodel.LobbyTransport

@Composable
fun LobbyScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF11221C),
                        Color(0xFF0C1814)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Back",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Default
                    )
                }
                Text(
                    "Game Lobby",
                    color = Color(0xFFF0E5CC),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            LobbyCard(
                title = "YOUR NAME",
                icon = Icons.Filled.Person,
                accentColor = Color(0xFF2EC08C)
            ) {
                val nameLocked = viewModel.hostedCode != null || viewModel.peers.isNotEmpty()
                OutlinedTextField(
                    value = viewModel.playerName,
                    onValueChange = { viewModel.playerName = it },
                    placeholder = { Text("Your name", color = Color.White.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White.copy(alpha = 0.5f),
                        focusedContainerColor = Color.White.copy(alpha = 0.12f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        disabledBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !nameLocked
                )
            }

            LobbyCard(
                title = "CONNECTION",
                icon = Icons.Filled.Public,
                accentColor = Color(0xFF8BBBEF)
            ) {
                val canSwitch = viewModel.hostedCode == null && viewModel.peers.isEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LobbyTransport.entries.forEach { option ->
                        val selected = viewModel.transport == option
                        Button(
                            onClick = { viewModel.switchTransport(option) },
                            enabled = canSwitch,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFF8BBBEF) else Color.White.copy(alpha = 0.06f),
                                contentColor = if (selected) Color(0xFF10221A) else Color.White.copy(alpha = 0.75f),
                                disabledContainerColor = if (selected) Color(0xFF8BBBEF).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.06f),
                                disabledContentColor = if (selected) Color(0xFF10221A) else Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(option.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
                Text(
                    if (viewModel.transport == LobbyTransport.ONLINE)
                        "Play across the internet via the relay server."
                    else
                        "Both players must be on the same Wi-Fi network.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LobbyCard(
                    title = "HOST",
                    icon = Icons.Filled.EmojiEvents,
                    accentColor = Color(0xFFF2BF4D),
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = viewModel.hostedCode ?: "",
                        onValueChange = {},
                        placeholder = { Text("CODE", color = Color.White.copy(alpha = 0.35f), textAlign = TextAlign.Center) },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            focusedBorderColor = Color.White.copy(alpha = 0.1f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false
                    )
                    val canHost = viewModel.hostedCode == null && viewModel.playerName.trim().isNotEmpty()
                    Button(
                        onClick = { viewModel.hostLobby() },
                        enabled = canHost,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF10221A)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFF2BF4D), // iOS: 0.95, 0.75, 0.30
                                            Color(0xFFE6A633)  // iOS: 0.90, 0.65, 0.20
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Create Game",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                LobbyCard(
                    title = "JOIN",
                    icon = Icons.Filled.ArrowCircleRight,
                    accentColor = Color(0xFF8BBBEF),
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = viewModel.joinCodeInput,
                        onValueChange = { viewModel.joinCodeInput = it.uppercase() },
                        placeholder = { Text("CODE", color = Color.White.copy(alpha = 0.35f), textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            focusedBorderColor = Color.White.copy(alpha = 0.1f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    val canJoin = viewModel.playerName.trim().isNotEmpty() &&
                        viewModel.joinCodeInput.trim().isNotEmpty() &&
                        viewModel.hostedCode == null
                    OutlinedButton(
                        onClick = { viewModel.joinLobby() },
                        enabled = canJoin,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF8BBBEF).copy(alpha = 0.22f),
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(Color(0xFF8BBBEF), Color(0xFF8BBBEF)))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            "Join",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            LobbyCard(
                title = "CONNECTION STATUS",
                icon = Icons.Filled.Wifi,
                accentColor = if (viewModel.peers.isEmpty()) Color.White.copy(alpha = 0.5f) else Color(0xFF2EC08C)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (viewModel.peers.isEmpty()) Color.White.copy(alpha = 0.3f) else Color(0xFF2EC08C),
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                    Text(viewModel.statusText, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
                }
                if (viewModel.peers.isEmpty()) {
                    Text("Waiting for players...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                } else {
                    viewModel.peers.forEach { peer ->
                        Text("• ${peer.displayName}", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
            }

            if (viewModel.hostedCode != null) {
                LobbyCard(
                    title = "START GAME",
                    icon = Icons.Filled.PlayArrow,
                    accentColor = Color(0xFF2EC08C)
                ) {
                    val playerCount = viewModel.gameState.players.size
                    Row {
                        Text("Players Ready", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "$playerCount",
                            color = if (playerCount >= 2) Color(0xFF2EC08C) else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.startGameAsHost() },
                        enabled = playerCount >= 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (playerCount >= 2) Color(0xFF2EC08C) else Color.White.copy(alpha = 0.1f),
                            contentColor = if (playerCount >= 2) Color(0xFF10221A) else Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Start Game",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            fontSize = 16.sp
                        )
                    }
                    if (playerCount < 2) {
                        Text("Need at least 2 players to start", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }
            }

            if (viewModel.lastError != null) {
                LobbyCard(
                    title = "ERROR",
                    icon = Icons.Filled.ErrorOutline,
                    accentColor = Color(0xFFF06666)
                ) {
                    Text(viewModel.lastError ?: "", color = Color(0xFFF06666), fontSize = 14.sp)
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun LobbyCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.04f),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Default,
                    fontSize = 14.sp,
                    letterSpacing = 0.2.sp
                )
            }
            content()
        }
    }
}
