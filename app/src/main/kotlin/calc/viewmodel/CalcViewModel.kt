package calc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import calc.engine.EngineFacade
import calc.engine.EvalResult
import calc.storage.HistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Display status for the current expression/result, per CALC-ARCH-01
 * §9 item 1: COMPUTING is a deliberate cosmetic floor (~80-100ms
 * minimum visible duration) for the Execution Pulse (CALC-01 §18),
 * not a reflection of real evaluation latency — evaluation itself is
 * sub-millisecond (CALC-ENGINE-01 §6/§7).
 *
 * ERROR corresponds to CALC-ENGINE-01 §8's three error kinds, all of
 * which map to the same 400ms red header pulse regardless of which
 * triggered it — this ViewModel doesn't need to distinguish them
 * further than EvalResult.Error already does.
 */
enum class CalcStatus { READY, COMPUTING, ERROR }

/**
 * Single immutable UI state per CALC-ARCH-01 §6 ("avoid nested
 * mutable state objects that cause unpredictable recomposition
 * scope").
 *
 * `expression` is the raw in-progress input string (CALC-ENGINE-01
 * §1: user builds a string of tokens, nothing reduced until `=` or
 * live preview). `preview` is the live running result per §6 — blank
 * or the last valid value while input is incomplete, "Undefined" if
 * complete-but-erroring. `result`/`resultRaw` are only set by a
 * committed `=` (onEquals), not by typing.
 */
data class CalcUiState(
    val expression: String = "",
    val preview: String = "",
    val result: String = "",
    val resultRaw: Double = 0.0,
    val status: CalcStatus = CalcStatus.READY
)

/**
 * Owns the current expression and live preview — the Tier 1 hot path
 * per CALC-ARCH-01 §4. Every step from keypress to StateFlow emission
 * is synchronous and in-memory; only the post-`=` history write drops
 * to Tier 2 (Dispatchers.IO, fire-and-forget, per §4's onEquals()
 * illustrative shape).
 *
 * historyRepository is the only Tier 2 dependency this ViewModel
 * needs directly — per §5's state ownership table, History/Memory UI
 * reads HistoryViewModel/MemoryViewModel independently and does not
 * route through CalcViewModel. CalcViewModel only *writes* to history
 * (on `=`), it never reads it back.
 */
class CalcViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalcUiState())
    val uiState: StateFlow<CalcUiState> = _uiState.asStateFlow()

    /**
     * Minimum visible duration for COMPUTING before flipping to
     * READY/ERROR, per CALC-ARCH-01 §9 item 1. Real evaluation
     * finishes well before this — the delay exists purely so the
     * Execution Pulse animation (CALC-01 §18) reads as intentional.
     */
    private val minComputingDurationMs = 90L

    /**
     * Appends a token to the expression and recomputes the live
     * preview, per CALC-ENGINE-01 §6. Synchronous, in-process, no
     * coroutine needed — EngineFacade.previewEvaluate is a plain
     * function call (sub-millisecond per spec).
     *
     * Does not touch `status`: per §6, incompleteness is never an
     * error state, and preview updates shouldn't trigger the
     * COMPUTING/Execution-Pulse treatment reserved for a committed
     * `=` (CALC-ARCH-01 §9 item 1 ties that pulse to onEquals, not to
     * every keystroke).
     */
    fun onKeyPress(token: String) {
        val newExpression = _uiState.value.expression + token
        val previewResult = EngineFacade.previewEvaluate(newExpression)

        _uiState.update {
            it.copy(
                expression = newExpression,
                preview = formatPreview(previewResult, fallback = it.preview)
            )
        }
    }

    /**
     * Removes the last token/character (⌫), re-running live preview
     * against the shortened expression. Same synchronous, no-coroutine
     * shape as onKeyPress.
     */
    fun onBackspace() {
        val current = _uiState.value.expression
        if (current.isEmpty()) return

        val newExpression = current.dropLast(1)
        val previewResult = EngineFacade.previewEvaluate(newExpression)

        _uiState.update {
            it.copy(
                expression = newExpression,
                preview = if (newExpression.isEmpty()) "" else formatPreview(previewResult, fallback = it.preview)
            )
        }
    }

    /**
     * All-clear (CALC-ENGINE-01 §7: "Current expression... Cleared by
     * AC"). Resets to a fresh CalcUiState — does not touch history or
     * memory, which are Tier 2 state owned elsewhere per §5.
     */
    fun onClear() {
        _uiState.value = CalcUiState()
    }

    /**
     * Commits the current expression as a final calculation (`=`).
     *
     * Tier 1 (synchronous): evaluate immediately via
     * EngineFacade.finalEvaluate, update displayed result right away.
     * A COMPUTING status is held for a cosmetic minimum duration
     * (§9 item 1) before settling to READY/ERROR — the *value* is
     * already computed and correct during that window, only the
     * status-color/pulse transition is gated.
     *
     * Tier 2 (async, fire-and-forget): on success only, write to
     * history on Dispatchers.IO via HistoryRepository, per §4's
     * onEquals() shape and CALC-DATA-01's "errors are never logged to
     * history" resolution (HistoryRepository's insert() signature
     * enforces this already — there's no error-insert path to call).
     */
    fun onEquals() {
        val expression = _uiState.value.expression
        if (expression.isBlank()) return

        val evalResult = EngineFacade.finalEvaluate(expression)

        viewModelScope.launch {
            _uiState.update { it.copy(status = CalcStatus.COMPUTING) }
            delay(minComputingDurationMs)

            when (evalResult) {
                is EvalResult.Value -> {
                    val formatted = EngineFacade.format(evalResult)
                    _uiState.update {
                        it.copy(
                            result = formatted,
                            resultRaw = evalResult.value,
                            preview = formatted,
                            status = CalcStatus.READY
                        )
                    }
                    // Displayed result is already updated above — this
                    // suspend call runs after that update, and
                    // HistoryRepository.insert dispatches on
                    // Dispatchers.IO internally, so it doesn't block
                    // this coroutine's caller or the UI thread. Not
                    // wrapped in a second launch: doing so would only
                    // let this call outlive a viewModelScope
                    // cancellation, which isn't a guarantee history
                    // writes need here.
                    historyRepository.insert(
                        expression = expression,
                        result = formatted,
                        resultRaw = evalResult.value
                    )
                }

                is EvalResult.Error -> {
                    val formatted = EngineFacade.format(evalResult)
                    _uiState.update {
                        it.copy(
                            result = formatted,
                            preview = formatted,
                            status = CalcStatus.ERROR
                        )
                    }
                }

                EvalResult.Incomplete -> {
                    // finalEvaluate() never returns Incomplete (see
                    // EngineFacade) — this branch exists only for
                    // sealed-class exhaustiveness.
                    _uiState.update { it.copy(status = CalcStatus.READY) }
                }
            }
        }
    }

    /**
     * Maps a preview EvalResult to a display string per CALC-ENGINE-01
     * §6: Incomplete keeps showing the last valid preview (never
     * blanks or errors mid-type); Value/Error format normally via
     * EngineFacade (Error here can only be a real evaluation error
     * like ÷0, since previewEvaluate collapses parse failures to
     * Incomplete already).
     */
    private fun formatPreview(result: EvalResult, fallback: String): String =
        when (result) {
            is EvalResult.Incomplete -> fallback
            else -> EngineFacade.format(result)
        }
}
