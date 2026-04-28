package com.alex.cabogame.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.alex.cabogame.ui.screens.*
import com.alex.cabogame.viewmodel.GameViewModel
import kotlinx.coroutines.delay

private enum class RootScreen { LOADING, MENU, LOBBY, HOW_TO_PLAY }

@Composable
fun RootScreen(viewModel: GameViewModel) {
    var screen by rememberSaveable { mutableStateOf(RootScreen.LOADING) }

    LaunchedEffect(Unit) {
        if (screen == RootScreen.LOADING) {
            delay(1_800)
            screen = RootScreen.MENU
        }
    }

    when {
        screen == RootScreen.LOADING -> SplashScreen()
        screen == RootScreen.HOW_TO_PLAY -> HowToPlayScreen(onBack = { screen = RootScreen.MENU })
        screen == RootScreen.MENU -> MainMenuScreen(
            onPlay = { screen = RootScreen.LOBBY },
            onHowToPlay = { screen = RootScreen.HOW_TO_PLAY }
        )
        viewModel.gameState.hasStarted -> GameTableScreen(viewModel = viewModel)
        else -> LobbyScreen(
            viewModel = viewModel,
            onBack = {
                viewModel.leaveLobby()
                screen = RootScreen.MENU
            }
        )
    }
}
