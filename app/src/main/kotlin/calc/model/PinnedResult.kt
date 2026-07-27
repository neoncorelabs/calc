package calc.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-pinned result, per CALC-DATA-01 §2.2 (CALC-01 §7: long-press
 * result -> Pin/Save).
 *
 * Kept as a separate table from [HistoryEntry] rather than a boolean
 * flag on it, per §2.2's own rationale: pins are a small, user-curated
 * list (closer to bookmarks than log entries), and this keeps the
 * potentially huge, unlimited history table from being queried every
 * time the UI just wants "show me my pins."
 *
 * No index needed here (unlike history_entry) — pins are a small,
 * user-curated set with no documented default sort/query pattern in
 * the spec beyond "get all," so an index would be premature.
 */
@Entity(tableName = "pinned_result")
data class PinnedResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val label: String?,

    val expression: String,

    val result: String,

    val resultRaw: Double,

    val pinnedAt: Long
)
