package calc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import calc.storage.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sources the Memory Drawer (CALC-UI-01 §10), per CALC-ARCH-01 §5's
 * state ownership table: "Memory M1-M3 | MemoryViewModel, sourced
 * from SettingsRepository | Tier 2".
 *
 * Deliberately reads from SettingsRepository, not PinnedRepository —
 * these are different concepts even though both involve "saving a
 * number." Memory registers (M1-M3) are exactly 3 fixed DataStore
 * slots, explicit save/recall, not auto-stacking (CALC-DATA-01 §3.1,
 * citing CALC-01 §10). PinnedResult (CALC-DATA-01 §2.2) is a separate,
 * unlimited, Room-backed list of long-press-saved calculations — a
 * different feature with its own repository. There's no
 * "PinnedViewModel" in this batch: CALC-ARCH-01 §5's ownership table
 * doesn't list one, only History and Memory. That looks like a real
 * gap in the table rather than a decision to skip pins (PinnedRepository
 * was built specifically for this in Batch B) — flagging it rather
 * than silently bundling an unrequested ViewModel into this batch.
 */
class MemoryViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * A single fixed-size (M1, M2, M3) memory state, each nullable —
     * null means "empty slot," matching CALC-DATA-01 §3.1's
     * null-means-empty semantics one-to-one.
     */
    data class MemoryState(
        val m1: Double?,
        val m2: Double?,
        val m3: Double?
    )

    /**
     * Combines all three DataStore-backed Flows into one StateFlow,
     * since the Memory Drawer (CALC-UI-01 §10) displays all three
     * slots together rather than one at a time — a single combined
     * state avoids the UI needing to collect three separate Flows
     * itself.
     */
    val memoryState: StateFlow<MemoryState> = combine(
        settingsRepository.memoryM1,
        settingsRepository.memoryM2,
        settingsRepository.memoryM3
    ) { m1, m2, m3 -> MemoryState(m1, m2, m3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MemoryState(m1 = null, m2 = null, m3 = null)
        )

    /**
     * Explicit store (MS-equivalent per CALC-01 §10's "explicit
     * save/recall, not auto-stacking" — this overwrites the slot, it
     * does not add to whatever's already there).
     */
    fun store(slot: MemorySlot, value: Double) {
        viewModelScope.launch {
            when (slot) {
                MemorySlot.M1 -> settingsRepository.setMemoryM1(value)
                MemorySlot.M2 -> settingsRepository.setMemoryM2(value)
                MemorySlot.M3 -> settingsRepository.setMemoryM3(value)
            }
        }
    }

    /**
     * Clears a slot back to empty (null), distinct from storing 0 —
     * CALC-DATA-01 §3.1: "nullable so the UI can distinguish 'empty
     * slot' from 'slot holding 0.'"
     */
    fun clear(slot: MemorySlot) {
        viewModelScope.launch {
            when (slot) {
                MemorySlot.M1 -> settingsRepository.setMemoryM1(null)
                MemorySlot.M2 -> settingsRepository.setMemoryM2(null)
                MemorySlot.M3 -> settingsRepository.setMemoryM3(null)
            }
        }
    }

    /**
     * NOTE — recall is intentionally not a method here, same
     * reasoning as HistoryViewModel's tap-to-load: recalling a memory
     * value means inserting it into CalcViewModel's active expression
     * (Tier 1), which is a different ViewModel's state per §5. The UI
     * layer reads memoryState here and calls into CalcViewModel
     * separately to actually insert the value — left for the UI
     * batch, once CalcViewModel has an insert-value-shaped entry
     * point and CALC-UI-01 §10's full interaction model has been
     * read (only skimmed the layout portion so far, for this batch).
     */
}

enum class MemorySlot { M1, M2, M3 }
