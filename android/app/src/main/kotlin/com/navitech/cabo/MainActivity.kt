package com.navitech.cabo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navitech.cabo.ui.RootScreen
import com.navitech.cabo.ui.theme.TableAppearanceStore
import com.navitech.cabo.ui.theme.CaboGameTheme
import com.navitech.cabo.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TableAppearanceStore.load(this)
        enableEdgeToEdge()
        setContent {
            CaboGameTheme {
                val vm: GameViewModel = viewModel()
                RootScreen(viewModel = vm)
            }
        }
    }
}
