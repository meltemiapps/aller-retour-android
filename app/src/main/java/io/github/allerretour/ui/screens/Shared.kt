package io.github.allerretour.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.Phrase
import io.github.allerretour.ui.Brand
import io.github.allerretour.ui.GlassButton
import io.github.allerretour.ui.GlassCard
import io.github.allerretour.ui.LocalDark
import io.github.allerretour.ui.Sub
import io.github.allerretour.ui.TAB_PLAN
import io.github.allerretour.ui.WithContentColor

/** Shown on Today and Money before any stop is added. */
@Composable
fun EmptyTrip(vm: AppViewModel) {
    GlassCard(title = "Welcome") {
        Text("Plan a trip of up to 10 stops, across any of the countries in the app.", fontSize = 16.sp)
        Sub("Everything stays on this device. No account, no tracking.")
        GlassButton("Add your first stop", prominent = true) { vm.tab = TAB_PLAN }
    }
}

/** Full-screen phrase in large text, to show someone. Tap anywhere to close. */
@Composable
fun PhraseCard(phrase: Phrase, onClose: () -> Unit) {
    val dark = LocalDark.current
    BackHandler(onBack = onClose)
    // White status bar icons on the blue card, put back on close.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val win = (view.context as? Activity)?.window
        val ctl = win?.let { WindowCompat.getInsetsController(it, view) }
        val was = ctl?.isAppearanceLightStatusBars
        ctl?.isAppearanceLightStatusBars = false
        onDispose { if (was != null) ctl.isAppearanceLightStatusBars = was }
    }
    run {
        WithContentColor(Color.White) {
            Column(
                Modifier.fillMaxSize().background(Brand.hero(dark))
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                    .systemBarsPadding().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Spacer(Modifier.weight(1f))
                Text(phrase.english, color = Color.White.copy(alpha = .9f), fontSize = 20.sp, textAlign = TextAlign.Center)
                Text(phrase.local, color = Color.White, fontSize = 56.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (phrase.pronunciation.isNotBlank()) Text(phrase.pronunciation, color = Color.White.copy(alpha = .92f), fontSize = 22.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(1f))
                GlassButton("Done", tint = Color.White, onClick = onClose)
            }
        }
    }
}
