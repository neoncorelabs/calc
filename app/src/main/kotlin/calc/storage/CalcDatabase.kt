package calc.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import calc.model.HistoryEntry
import calc.model.PinnedResult

/**
 * Room database for CALC, per CALC-DATA-01 §2/§5. Two tables:
 * history_entry and pinned_result — see [HistoryEntry], [PinnedResult].
 *
 * version = 1: first schema, no migrations needed yet.
 * exportSchema = true (Room's default) so schema JSON snapshots are
 * written to the schemas/ directory configured in app/build.gradle.kts
 * (`room { schemaDirectory(...) }`), for future migration validation.
 */
@Database(
    entities = [HistoryEntry::class, PinnedResult::class],
    version = 1,
    exportSchema = true
)
abstract class CalcDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun pinnedDao(): PinnedDao
}
