package calc.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single completed calculation, per CALC-DATA-01 §2.1.
 *
 * Only successful calculations are stored — per §6 item 2 (resolved),
 * Undefined/Overflow results are NOT logged to history at all. There is
 * no `isError` field: the §2.1 table originally listed one, but §5's
 * schema diagram and §6's resolution supersede that draft and omit it.
 *
 * `result` is stored as the already-formatted display string (not just
 * `resultRaw`) so history cards always show exactly what the user saw
 * at calculation time, even if display/formatting rules change in a
 * later app version (§2.1.1). `resultRaw` is kept alongside so
 * "tap to reload calculation" (CALC-01 §8) can resume from full
 * precision rather than re-parsing a rounded display string.
 *
 * Indexed on `timestamp` since `HistoryDao.getAll()` (§2.3) orders by
 * it descending for the History screen.
 */
@Entity(
    tableName = "history_entry",
    indices = [Index(value = ["timestamp"])]
)
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val expression: String,

    val result: String,

    val resultRaw: Double,

    val timestamp: Long
)
