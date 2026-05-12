package com.navitech.cabo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.navitech.cabo.ui.screens.*
import com.navitech.cabo.viewmodel.GameViewModel
import kotlinx.coroutines.delay

private enum class RootRoute { LOADING, MENU, LOBBY, HOW_TO_PLAY, APPEARANCE }

@Composable
fun RootScreen(viewModel: GameViewModel) {
    var screen by rememberSaveable { mutableStateOf(RootRoute.LOADING) }

    BackHandler(enabled = screen == RootRoute.HOW_TO_PLAY || screen == RootRoute.APPEARANCE) {
        screen = RootRoute.MENU
    }
    BackHandler(enabled = screen == RootRoute.LOBBY && !viewModel.gameState.hasStarted) {
        viewModel.leaveLobby()
        screen = RootRoute.MENU
    }

    LaunchedEffect(Unit) {
        if (screen == RootRoute.LOADING) {
            delay(1_800)
            screen = RootRoute.MENU
        }
    }

    when {
        screen == RootRoute.LOADING -> SplashScreen()
        screen == RootRoute.HOW_TO_PLAY -> HowToPlayScreen(onBack = { screen = RootRoute.MENU })
        screen == RootRoute.APPEARANCE -> AppearanceSettingsScreen(onBack = { screen = RootRoute.MENU })
        screen == RootRoute.MENU -> MainMenuScreen(
            onPlay = { screen = RootRoute.LOBBY },
            onHowToPlay = { screen = RootRoute.HOW_TO_PLAY },
            onTableLook = { screen = RootRoute.APPEARANCE },
        )
        viewModel.gameState.hasStarted -> GameTableScreen(
            viewModel = viewModel,
            onLeaveGame = { viewModel.leaveLobby() }
        )
        else -> LobbyScreen(
            viewModel = viewModel,
            onBack = {
                viewModel.leaveLobby()
                screen = RootRoute.MENU
            }
        )
    }
}
