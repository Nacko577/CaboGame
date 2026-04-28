package com.alex.cabogame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alex.cabogame.ui.RootScreen
import com.alex.cabogame.ui.theme.CaboGameTheme
import com.alex.cabogame.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaboGameTheme {
                val vm: GameViewModel = viewModel()
                RootScreen(viewModel = vm)
            }
        }
    }
}
