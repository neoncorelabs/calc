package calc.viewmodel

import neoncore.components.NeonStatus
import neoncore.theme.NeonAccent

/**
 * Screen-level status composition, per CALC-UI-01 §2's six states
 * (READY, COMPUTING, MEMORY ACTIVE, ERROR, SCIENTIFIC MODE, HISTORY
 * OPEN) and an explicit architectural decision: the System Status
 * Header is a *presentation* concept that spans navigation and mode,
 * not pure calculation state, so it must not live inside
 * CalcViewModel alone.
 *
 * Ownership split:
 *   - `calculation` (READY / COMPUTING / ERROR) comes from
 *     CalcViewModel's existing CalcStatus — calculation state was
 *     already correctly scoped there from Batch C and isn't
 *     duplicated or renamed here.
 *   - `historyOpen` and `scientificMode` are screen/navigation-level
 *     booleans that CalcViewModel has no business knowing about —
 *     they belong to whatever composable owns screen navigation
 *     (e.g. a CalcScreen composable holding its own remembered state,
 *     or a future light-weight ScreenViewModel if navigation state
 *     grows more complex than two booleans). CalcScreenStatus doesn't
 *     assume which; it just composes whatever the three inputs are.
 *
 * This keeps CalcViewModel, HistoryViewModel, and screen/navigation
 * state each producing only their own domain signal — this file is
 * the "screen controller composes them" step, not a fourth owner of
 * new state.
 */
data class CalcScreenStatus(
    val calculation: CalcStatus,
    val historyOpen: Boolean,
    val scientificMode: Boolean
) {
    /**
     * Resolves the composed screen state into the single NeonStatus
     * the shared neon-core StatusHeader composable expects.
     *
     * Precedence (highest to lowest), since only one status label can
     * show at a time in the header:
     *   1. ERROR — a calculation error always takes priority over
     *      mode/navigation state; CALC-UI-01 §13 ties it to the
     *      distinct 400ms red header pulse (NeonMotion.ERROR_PULSE_MS
     *      elsewhere), which shouldn't be pre-empted by e.g. history
     *      being open.
     *   2. COMPUTING — mid-calculation feedback is also higher
     *      priority than a static mode/navigation indicator.
     *   3. HISTORY OPEN — navigation state, shown while the history
     *      drawer/sheet is up.
     *   4. MEMORY ACTIVE — not derived from any of this class's three
     *      fields yet. CALC-UI-01 §2 lists it as a distinct state,
     *      but neither CALC-ARCH-01 §5 nor the UI spec's Memory
     *      Drawer section (§10) specifies exactly what triggers it
     *      (memory drawer open? any Mn slot non-null? a value just
     *      recalled?) — left unresolved deliberately rather than
     *      guessing; see NOTE below.
     *   5. SCIENTIFIC MODE — mode state, shown when in scientific
     *      layout and nothing higher-priority is active.
     *   6. READY — default/fallback.
     *
     * NOTE on MEMORY ACTIVE: intentionally not wired into this
     * resolution yet. Flagging rather than inventing a trigger
     * condition — this needs a product decision (most likely "any of
     * M1-M3 is non-null," sourced from MemoryViewModel.memoryState,
     * which would mean this function needs a fourth input). Left for
     * whichever batch actually builds the Memory Drawer, so the
     * decision is made against real UI code rather than guessed here.
     */
    fun toNeonStatus(): NeonStatus = when {
        calculation == CalcStatus.ERROR -> CalcNeonStatus.Error
        calculation == CalcStatus.COMPUTING -> CalcNeonStatus.Computing
        historyOpen -> CalcNeonStatus.HistoryOpen
        scientificMode -> CalcNeonStatus.ScientificMode
        else -> CalcNeonStatus.Ready
    }
}

/**
 * CALC-01's six-state status vocabulary (CALC-UI-01 §2), expressed as
 * NeonStatus values per StatusHeader.kt's own doc comment: "Apps may
 * define their own NeonStatus values — the constraint is that color
 * must always carry the same meaning."
 *
 * Ready/Computing/Error intentionally reuse the shared NeonStatus
 * companion object's existing values rather than redefining them —
 * CALC-01's labels and colors for these three are identical to the
 * provided starting vocabulary (READY/green, COMPUTING/blue,
 * ERROR/red). MemoryActive/ScientificMode/HistoryOpen are new: the
 * shared vocabulary's "ACTIVE" (magenta) and no blue/green
 * mode-labeled equivalents don't match CALC-01 §2's specific label
 * text, so those three are defined fresh here against NeonAccent
 * directly, with colors taken from §2's table.
 */
object CalcNeonStatus {
    val Ready = NeonStatus.Ready
    val Computing = NeonStatus.Computing
    val Error = NeonStatus.Error

    val MemoryActive = NeonStatus("MEMORY ACTIVE", NeonAccent.Magenta)
    val ScientificMode = NeonStatus("SCIENTIFIC MODE", NeonAccent.Green)
    val HistoryOpen = NeonStatus("HISTORY OPEN", NeonAccent.Blue)
}
