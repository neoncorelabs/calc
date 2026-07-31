package calc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import calc.viewmodel.AppContainer
import calc.viewmodel.CalcScreenStatus
import calc.viewmodel.CalcStatus
import calc.viewmodel.CalcViewModel
import calc.viewmodel.HistoryViewModel
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
 * EXECUTION PULSE (§18, revised): this composable owns detecting the
 * COMPUTING -> READY transition and driving `pulseTrigger`, which
 * CalcDisplay uses to play the visual fade + sweep-line animation.
 *
 * CALC does not use haptic or sound feedback — explicit product
 * decision, revising §15/§18 of CALC-UI-01: visual transitions (this
 * Execution Pulse, live preview, status color) are the calculator's
 * only feedback channel. This is CALC-specific; the underlying
 * settings infrastructure (SettingsRepository/PreferencesManager)
 * still supports hapticsEnabled/soundEnabled unchanged, for other
 * NeonCoreLabs apps — SettingsViewModel simply isn't consumed by this
 * screen for that purpose (an earlier version of this file did wire a
 * completion haptic through SettingsViewModel.hapticsEnabled; removed
 * in the same change that revised the spec).
 */
@Composable
fun CalcScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val factory = remember(container) { calcViewModelFactory(container) }
    val calcViewModel: CalcViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val uiState by calcViewModel.uiState.collectAsState()
    val historyEntries by historyViewModel.history.collectAsState()

    // Screen-level navigation/mode state — NOT owned by CalcViewModel.
    var historyOpen by remember { mutableStateOf(false) }
    var scientificMode by remember { mutableStateOf(false) }

    // Execution Pulse trigger: an ever-increasing counter, passed to
    // CalcDisplay, incremented only on a genuine COMPUTING -> READY
    // edge (see the LaunchedEffect below) — not on every recomposition
    // where status happens to equal READY. Drives the visual fade +
    // sweep only; no haptic is fired (see class doc comment above).
    var pulseTrigger by remember { mutableIntStateOf(0) }
    var previousStatus by remember { mutableStateOf(CalcStatus.READY) }

    LaunchedEffect(uiState.status) {
        if (previousStatus == CalcStatus.COMPUTING && uiState.status == CalcStatus.READY) {
            pulseTrigger++
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
            // TEMPORARY: tap the header to toggle History Screen open,
            // purely so this batch's HistoryScreen is reachable for
            // verification before the real swipe-down gesture (§7) is
            // built in the later gesture-handling priority. Remove
            // this clickable wrapper (not the StatusHeader itself)
            // once that gesture replaces it — not a spec requirement,
            // just a throwaway trigger per this batch's own note not
            // to over-invest here.
            Box(modifier = Modifier.clickable { historyOpen = !historyOpen }) {
                StatusHeader(
                    moduleName = "CALC",
                    subtitle = "Precision Engine",
                    status = screenStatus.toNeonStatus()
                )
            }

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

        // History Screen (CALC-UI-01 §8): shown as a full-screen
        // overlay swapped in over the Home Screen content above, while
        // historyOpen is true. No spec dictates the transition
        // mechanism (overlay vs. separate composable vs. nav) — this
        // is the simplest option that keeps CalcScreen's existing
        // Home Screen structure untouched rather than a larger
        // rewrite, per this batch's explicit scope.
        //
        // Nothing currently SETS historyOpen = true — that's the
        // swipe-down gesture (§7), explicitly deferred to the later
        // gesture-handling priority. historyOpen is still wired
        // through end-to-end (state -> StatusHeader label -> this
        // overlay) so plugging in that gesture later is a one-line
        // change, not a restructure.
        if (historyOpen) {
            HistoryScreen(
                history = historyEntries,
                onLoadEntry = { entry -> calcViewModel.loadFromHistory(entry) },
                onDeleteEntry = { id -> historyViewModel.deleteEntry(id) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeonDark.Background0)
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
