package com.navitech.cabo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.navitech.cabo.ui.screens.*
import com.navitech.cabo.viewmodel.GameViewModel
import kotlinx.coroutines.delay

private enum class RootScreen { LOADING, MENU, LOBBY, HOW_TO_PLAY }

@Composable
fun RootScreen(viewModel: GameViewModel) {
    var screen by rememberSaveable { mutableStateOf(RootScreen.LOADING) }

    BackHandler(enabled = screen == RootScreen.HOW_TO_PLAY) {
        screen = RootScreen.MENU
    }
    BackHandler(enabled = screen == RootScreen.LOBBY && !viewModel.gameState.hasStarted) {
        viewModel.leaveLobby()
        screen = RootScreen.MENU
    }

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
        viewModel.gameState.hasStarted -> GameTableScreen(
            viewModel = viewModel,
            onLeaveGame = { viewModel.leaveLobby() }
        )
        else -> LobbyScreen(
            viewModel = viewModel,
            onBack = {
                viewModel.leaveLobby()
                screen = RootScreen.MENU
            }
        )
    }
}
