package io.github.allerretour.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.add
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.ui.screens.GuideScreen
import io.github.allerretour.ui.screens.KitScreen
import io.github.allerretour.ui.screens.MoneyScreen
import io.github.allerretour.ui.screens.PlanScreen
import io.github.allerretour.ui.screens.TodayScreen

private data class Tab(val label: String, val icon: ImageVector)

/** Five tabs, as on iPhone. Safety lives inside Guide as its first section. */
private val tabs = listOf(
    Tab("Today", Icons.Outlined.Schedule),
    Tab("Plan", Icons.Outlined.Map),
    Tab("Money", Icons.Outlined.Payments),
    Tab("Guide", Icons.AutoMirrored.Outlined.MenuBook),
    Tab("Kit", Icons.Outlined.Checklist),
)

const val TAB_TODAY = 0
const val TAB_PLAN = 1

@Composable
fun AllerRetourApp(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val photo by vm.photo.collectAsStateWithLifecycle()
    val dark = LocalDark.current
    val backdrop = remember { BackdropState() }
    SideEffect {
        backdrop.kind = BackdropKind.of(s.backdrop)
        backdrop.dark = dark
        backdrop.photo = photo
    }
    BackHandler(enabled = vm.tab != TAB_TODAY) { vm.tab = TAB_TODAY }

    CompositionLocalProvider(
        LocalBackdrop provides backdrop,
        LocalTinted provides (s.glass == "Tinted"),
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.fillMaxSize().onSizeChanged { backdrop.size = it.toSize() }) {
            Backdrop(backdrop)
            when (vm.tab) {
                0 -> TodayScreen(vm)
                1 -> PlanScreen(vm)
                2 -> MoneyScreen(vm)
                3 -> GuideScreen(vm)
                else -> KitScreen(vm)
            }
            // Frosted strip under the status bar, so scrolled cards don't run into the clock.
            val edge = if (dark) Brand.Navy else androidx.compose.ui.graphics.Color.White
            Box(
                Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 22.dp)))
                    .background(Brush.verticalGradient(0f to edge.copy(alpha = .96f), .62f to edge.copy(alpha = .88f), 1f to edge.copy(alpha = 0f))),
            )
            // ⓘ stays put while the page scrolls, like the toolbar button on iOS.
            InfoButton(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 6.dp, end = 16.dp)) { vm.showAbout = true }
            TabBar(vm.tab, { vm.tab = it }, Modifier.align(Alignment.BottomCenter))
        }
        vm.phrase?.let { io.github.allerretour.ui.screens.PhraseCard(it) { vm.phrase = null } }
        if (vm.showAbout) SettingsSheet(vm) { vm.showAbout = false }
    }
}

/** Floating, rounded, translucent tab bar. */
@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth().height(64.dp),
        RoundedCornerShape(32.dp), tinted = true,
    ) {
        Row(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            tabs.forEachIndexed { i, t ->
                val on = i == selected
                val color = if (on) accent else LocalContentColor.current
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp))
                        .background(if (on) accent.copy(alpha = .14f) else androidx.compose.ui.graphics.Color.Transparent)
                        .semantics { this.selected = on }
                        .clickable(role = Role.Tab) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, null, tint = color, modifier = Modifier.size(24.dp))
                    Text(t.label, color = color, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
