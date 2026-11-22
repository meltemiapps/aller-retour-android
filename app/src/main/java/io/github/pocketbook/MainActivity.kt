package io.github.pocketbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pocketbook.ui.PocketbookApp
import io.github.pocketbook.ui.PocketbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val s by vm.state.collectAsStateWithLifecycle()
            PocketbookTheme(useWallpaperColours = s.useWallpaperColours) { PocketbookApp(vm) }
        }
    }
}
