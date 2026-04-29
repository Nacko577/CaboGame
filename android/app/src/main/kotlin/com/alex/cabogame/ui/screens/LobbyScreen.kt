package com.alex.cabogame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.cabogame.ui.theme.*
import com.alex.cabogame.viewmodel.GameViewModel

@Composable
fun LobbyScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141F1A),
                        Color(0xFF0A1410)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Back",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
                
                Text(
                    "Game Lobby",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF2E6CC),
                                Color(0xFFD9C7A6)
                            )
                        )
                    )
                )
                
                // Spacer for balance
                Spacer(Modifier.width(70.dp))
            }
            
            // Player Name Card
            LobbyCard(
                icon = Icons.Default.Person,
                title = "YOUR NAME",
                accentColor = AccentGreen
            ) {
                OutlinedTextField(
                    value = viewModel.playerName,
                    onValueChange = { viewModel.playerName = it },
                    placeholder = {
                        Text(
                            "Enter your name",
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        focusedBorderColor = Color.White.copy(alpha = 0.1f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Host and Join Cards side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Host Card
                LobbyCard(
                    icon = Icons.Default.Star,
                    title = "HOST",
                    accentColor = AccentGold,
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = { viewModel.hostLobby() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(AccentGold, AccentGoldDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Create Game",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F1A14)
                                )
                            )
                        }
                    }
                    
                    if (viewModel.hostedCode != null) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Code:",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            Text(
                                viewModel.hostedCode ?: "",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentGold
                                )
                            )
                        }
                    }
                }
                
                // Join Card
                LobbyCard(
                    icon = Icons.Default.ArrowForward,
                    title = "JOIN",
                    accentColor = AccentBlue,
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = viewModel.joinCodeInput,
                        onValueChange = { viewModel.joinCodeInput = it.uppercase() },
                        placeholder = {
                            Text(
                                "CODE",
                                color = Color.White.copy(alpha = 0.35f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            focusedBorderColor = Color.White.copy(alpha = 0.1f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    OutlinedButton(
                        onClick = { viewModel.joinLobby() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AccentBlue.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AccentBlue.copy(alpha = 0.5f),
                                    AccentBlue.copy(alpha = 0.5f)
                                )
                            )
                        )
                    ) {
                        Text(
                            "Join",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            // Connection Status Card
            LobbyCard(
                icon = Icons.Default.Wifi,
                title = "CONNECTION STATUS",
                accentColor = if (viewModel.peers.isEmpty()) Color.White.copy(alpha = 0.5f) else AccentGreen
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (viewModel.peers.isEmpty()) Color.White.copy(alpha = 0.3f) else AccentGreen,
                                CircleShape
                            )
                    )
                    Text(
                        viewModel.statusText,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    )
                }
                
                if (viewModel.peers.isEmpty()) {
                    Text(
                        "Waiting for players...",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.peers.forEach { peer ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AccentGreen, CircleShape)
                                )
                                Text(
                                    peer.displayName,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Start Game Section (Host only)
            if (viewModel.hostedCode != null) {
                val playerCount = viewModel.gameState.players.size
                
                LobbyCard(
                    icon = Icons.Default.PlayArrow,
                    title = "START GAME",
                    accentColor = AccentGreen
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Players Ready",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            "$playerCount",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (playerCount >= 2) AccentGreen else Color.White.copy(alpha = 0.4f)
                            )
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.startGameAsHost() },
                        enabled = playerCount >= 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .then(
                                if (playerCount >= 2) Modifier.shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    ambientColor = AccentGreen.copy(alpha = 0.4f),
                                    spotColor = AccentGreen.copy(alpha = 0.4f)
                                ) else Modifier
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (playerCount >= 2) {
                                        Brush.verticalGradient(
                                            colors = listOf(AccentGreen, AccentGreenDark)
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.1f),
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (playerCount >= 2) Color(0xFF0F1A14) else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Start Game",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playerCount >= 2) Color(0xFF0F1A14) else Color.White.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                    
                    if (playerCount < 2) {
                        Text(
                            "Need at least 2 players to start",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Error Section
            if (viewModel.lastError != null) {
                LobbyCard(
                    icon = Icons.Default.Warning,
                    title = "ERROR",
                    accentColor = AccentRed
                ) {
                    Text(
                        viewModel.lastError ?: "",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentRed
                        )
                    )
                }
            }
            
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun LobbyCard(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.04f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                )
            }
            
            content()
        }
    }
}
