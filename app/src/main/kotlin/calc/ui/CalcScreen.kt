package calc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import calc.viewmodel.AppContainer
import calc.viewmodel.CalcScreenStatus
import calc.viewmodel.CalcStatus
import calc.viewmodel.CalcViewModel
import calc.viewmodel.SettingsViewModel
import calc.viewmodel.calcViewModelFactory
import neoncore.components.StatusHeader
import neoncore.theme.NeonDark
import neoncore.theme.NeonSpacing

/**
 * Composition root for the Home Screen (CALC-UI-01 §3/§4):
 * System Header -> Display -> Divider -> Keypad, nothing else, per
 * §3's Information Hierarchy ("Nothing else. No unnecessary
 * buttons.").
 *
 * Owns historyOpen/scientificMode here as plain remembered Boolean
 * state — the screen-level state from the explicit ownership decision
 * (Batch F): these are navigation/mode booleans CalcViewModel has no
 * business knowing about. Kept as simple `remember { mutableStateOf }`
 * rather than a dedicated ScreenViewModel, since two booleans doesn't
 * yet justify a new ViewModel class — matches CalcScreenStatus's own
 * doc comment ("or a future light-weight ScreenViewModel if
 * navigation state grows more complex than two booleans").
 *
 * NOTE: historyOpen/scientificMode are currently plumbed through but
 * not yet ACTED on — there's no History screen or Scientific Mode
 * keypad composable to switch to yet (those are later UI batches).
 * The state and the StatusHeader label are wired correctly now so
 * that plugging in those screens later doesn't require touching this
 * composition root's structure again, but toggling either currently
 * only changes the header label, nothing else on screen. Flagging so
 * this isn't mistaken for a finished multi-screen flow.
 *
 * EXECUTION PULSE (§18): this composable owns detecting the
 * COMPUTING -> READY transition (step 4 is literally "the System
 * Status changes," which only this level can observe since it's the
 * one composing CalcScreenStatus) and firing the completion haptic
 * (step 5), gated on SettingsViewModel.hapticsEnabled. Per an
 * explicit decision: ONLY COMPUTING -> READY triggers the haptic —
 * COMPUTING -> ERROR does not, since §18 never mentions error
 * completions and §13's error handling is a separate, simpler
 * animation (400ms red header pulse only, no fade/transition/sweep).
 * The "Error -> Double pulse" haptic from §15's table is a distinct,
 * later concern (per-button-press haptics as a whole, not wired in
 * this batch).
 */
@Composable
fun CalcScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val factory = remember(container) { calcViewModelFactory(container) }
    val calcViewModel: CalcViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val uiState by calcViewModel.uiState.collectAsState()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()
    val haptics = LocalHapticFeedback.current

    // Screen-level navigation/mode state — NOT owned by CalcViewModel.
    var historyOpen by remember { mutableStateOf(false) }
    var scientificMode by remember { mutableStateOf(false) }

    // Execution Pulse trigger: an ever-increasing counter, passed to
    // CalcDisplay, incremented only on a genuine COMPUTING -> READY
    // edge (see the LaunchedEffect below) — not on every recomposition
    // where status happens to equal READY.
    var pulseTrigger by remember { mutableIntStateOf(0) }
    var previousStatus by remember { mutableStateOf(CalcStatus.READY) }

    LaunchedEffect(uiState.status) {
        if (previousStatus == CalcStatus.COMPUTING && uiState.status == CalcStatus.READY) {
            pulseTrigger++
            if (hapticsEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
        previousStatus = uiState.status
    }

    val screenStatus = CalcScreenStatus(
        calculation = uiState.status,
        historyOpen = historyOpen,
        scientificMode = scientificMode
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = NeonDark.Background0
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusHeader(
                moduleName = "CALC",
                subtitle = "Precision Engine",
                status = screenStatus.toNeonStatus()
            )

            CalcDisplay(
                expression = uiState.expression,
                result = uiState.preview.ifEmpty { uiState.result },
                pulseTrigger = pulseTrigger
            )

            // Divider: 1dp, divider color, separates display from
            // keypad (§5).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NeonDark.Divider)
            )

            CalcKeypad(
                onAction = { action -> handleKeyAction(action, calcViewModel) },
                modifier = Modifier.padding(
                    horizontal = NeonSpacing.MarginHorizontal,
                    vertical = NeonSpacing.Medium
                )
            )
        }
    }
}

private fun handleKeyAction(action: CalcKeyAction, viewModel: CalcViewModel) {
    when (action) {
        is CalcKeyAction.Insert -> viewModel.onKeyPress(action.text)
        is CalcKeyAction.Clear -> viewModel.onClear()
        is CalcKeyAction.Equals -> viewModel.onEquals()
    }
}
