package calc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import calc.model.HistoryEntry
import calc.model.PinnedResult
import calc.storage.HistoryRepository
import calc.storage.PinnedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sources the History screen (CALC-UI-01 §8) — both the history list
 * itself and pinned results, per an explicit scoping decision:
 * **a ViewModel should model a screen, not a table.**
 *
 * CALC-ARCH-01 §5's state ownership table only names History and
 * Memory rows, with no PinnedResult/PinnedRepository entry at all —
 * that's a genuine gap in the table (flagged in Batch D), not a
 * decision to cut pins from scope; PinnedRepository was built
 * specifically for "long-press result -> Pin/Save" (CALC-DATA-01
 * §2.2, CALC-01 §7). Rather than resolve that gap unilaterally, the
 * decision was made explicitly: fold pins into HistoryViewModel
 * rather than create a separate PinnedViewModel, because CALC-UI-01
 * describes pinning only as an action performed *from* a result
 * (§7's long-press), with no evidence yet of a dedicated Pinned
 * screen or nav destination. HistoryRepository and PinnedRepository
 * stay separate (separate tables, separate DAOs, separate query
 * performance characteristics per §2.2's own rationale) — only the
 * ViewModel composes both streams into one screen's state. If a
 * later UI revision introduces a standalone Pinned screen,
 * extracting a PinnedViewModel is a small refactor (move the
 * `pinned` StateFlow and pin-related methods into a new class) since
 * the repository boundary already exists and nothing in the
 * persistence layer needs to change.
 *
 * Reads both repositories' Flows independently — does not route
 * through CalcViewModel (§5: "keeping the hot-path ViewModel free of
 * responsibilities unrelated to typing speed").
 */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val pinnedRepository: PinnedRepository
) : ViewModel() {

    /**
     * History list, newest first (HistoryDao.getAll() orders by
     * timestamp DESC — see CALC-DATA-01 §2.3). Exposed as a
     * StateFlow (not the raw Flow) so the UI always has a
     * synchronously-readable current value, with an empty list
     * before the first DB emission arrives rather than nothing at
     * all.
     *
     * Note: §5's table phrasing is "HistoryRepository.getAll()", but
     * the real HistoryRepository exposes this as a property — `val
     * history: Flow<List<HistoryEntry>>` — not a getAll() method.
     * Same "spec illustrative, code actual" situation as the rest of
     * this project; used the real property name.
     *
     * SharingStarted.WhileSubscribed(5000): keeps collecting the
     * underlying Room Flow for 5s after the last UI subscriber goes
     * away (e.g. a brief configuration change), so a quick navigate-
     * away-and-back doesn't pay the cost of a fresh query. Standard
     * pattern, not something CALC-ARCH-01 specifies explicitly.
     */
    val history: StateFlow<List<HistoryEntry>> = historyRepository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Pinned results, sourced from PinnedRepository — same StateFlow
     * treatment as `history` above, for the same reasons.
     * PinnedDao.getAll() has no explicit ordering (CALC-DATA-01 §2.2:
     * "no documented default sort/query pattern in the spec beyond
     * 'get all'"), so this list's order is whatever Room/SQLite
     * returns, unmodified here.
     */
    val pinned: StateFlow<List<PinnedResult>> = pinnedRepository.pinned
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Swipe-to-delete a single history entry (CALC-UI-01 §8: "Swipe /
     * Delete"). No bulk-clear exists — CALC-DATA-01 §2.3 explicitly
     * decided against a deleteAll() in v1, so there's intentionally
     * no corresponding method here either.
     */
    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    /**
     * Long-press a result to pin/save it (CALC-UI-01 §7). label is
     * optional at creation time — matches PinnedResult.label's
     * nullability (CALC-DATA-01 §2.2); the user can add a label later
     * via updatePinLabel.
     */
    fun pinResult(label: String?, expression: String, result: String, resultRaw: Double) {
        viewModelScope.launch {
            pinnedRepository.insert(
                label = label,
                expression = expression,
                result = result,
                resultRaw = resultRaw
            )
        }
    }

    /** Removes a pin. Does not touch the corresponding history entry, if any — separate tables, separate lifecycles. */
    fun unpin(id: Long) {
        viewModelScope.launch {
            pinnedRepository.deleteById(id)
        }
    }

    /**
     * Renames (or clears, via label = null) an existing pin's label
     * after creation.
     */
    fun updatePinLabel(id: Long, label: String?) {
        viewModelScope.launch {
            pinnedRepository.updateLabel(id, label)
        }
    }

    /**
     * NOTE — not implemented here, flagging rather than guessing:
     * CALC-UI-01 §8 also lists "Tap / Loads calculation" — tapping a
     * history card should load that expression back into the active
     * calculator. Presumably tapping a pinned card does the same.
     * That interaction crosses from HistoryViewModel's Tier 2 state
     * into CalcViewModel's Tier 1 expression state (§5's ownership
     * table keeps these deliberately separate), so it likely belongs
     * as a callback the UI layer wires between the two ViewModels
     * (e.g. HistoryScreen calls calcViewModel.loadFromHistory(entry)
     * on tap) rather than as a method on HistoryViewModel itself.
     * Left for the UI batch, since CalcViewModel doesn't yet have a
     * loadFromHistory()-shaped entry point and CALC-UI-01 hasn't been
     * read in full yet per the project's stated order (UI phase not
     * started).
     */
}
