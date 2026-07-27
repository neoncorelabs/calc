package calc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import calc.model.HistoryEntry
import calc.storage.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sources the History screen (CALC-UI-01 §8), per CALC-ARCH-01 §5's
 * state ownership table: "History list | HistoryViewModel, sourced
 * from HistoryRepository... | Tier 2". Reads its repository's Flow
 * independently — does not route through CalcViewModel (§5: "keeping
 * the hot-path ViewModel free of responsibilities unrelated to typing
 * speed").
 *
 * Note: the table's exact phrasing is "HistoryRepository.getAll()",
 * but the real HistoryRepository (Batch B) exposes this as a
 * property — `val history: Flow<List<HistoryEntry>>` — not a
 * getAll() method. Same "spec illustrative, code actual" situation
 * as the rest of this project; used the real property name.
 */
class HistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    /**
     * History list, newest first (HistoryDao.getAll() orders by
     * timestamp DESC — see CALC-DATA-01 §2.3). Exposed as a
     * StateFlow (not the raw Flow) so the UI always has a
     * synchronously-readable current value, with an empty list
     * before the first DB emission arrives rather than nothing at
     * all — avoids the UI needing to handle a null/absent state
     * before Compose can collect the first item.
     *
     * SharingStarted.WhileSubscribed(5000): keeps collecting the
     * underlying Room Flow for 5s after the last UI subscriber goes
     * away (e.g. a brief configuration change), so a quick navigate-
     * away-and-back doesn't pay the cost of a fresh query. This is a
     * standard pattern, not something CALC-ARCH-01 specifies
     * explicitly — noting it here in case a future session wants to
     * revisit the timeout value.
     */
    val history: StateFlow<List<HistoryEntry>> = historyRepository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Swipe-to-delete a single entry (CALC-UI-01 §8: "Swipe / Delete").
     * No bulk-clear exists — CALC-DATA-01 §2.3 explicitly decided
     * against a deleteAll() in v1, so there's intentionally no
     * corresponding method here either.
     */
    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    /**
     * NOTE — not implemented here, flagging rather than guessing:
     * CALC-UI-01 §8 also lists "Tap / Loads calculation" — tapping a
     * history card should load that expression back into the active
     * calculator. That interaction crosses from HistoryViewModel's
     * Tier 2 state into CalcViewModel's Tier 1 expression state
     * (§5's ownership table keeps these deliberately separate), so
     * it likely belongs as a callback the UI layer wires between the
     * two ViewModels (e.g. HistoryScreen calls
     * calcViewModel.loadFromHistory(entry) on tap) rather than as a
     * method on HistoryViewModel itself. Left for the UI batch, since
     * CalcViewModel doesn't yet have a loadFromHistory()-shaped entry
     * point and CALC-UI-01 hasn't been read in full yet per the
     * project's stated order (UI phase not started).
     */
}
