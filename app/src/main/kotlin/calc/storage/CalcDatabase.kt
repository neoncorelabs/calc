package calc.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    companion object {
        /**
         * Standard single-instance Room builder, added here (rather than
         * inline in AppContainer) so the database's own construction
         * details — file name, singleton guarding — live next to the
         * class they construct. AppContainer just calls this.
         *
         * Not previously present: CALC-ARCH-01 §7's AppContainer snippet
         * is illustrative only and assumes a `CalcDatabase.build(context)`
         * factory that didn't exist yet in this class.
         */
        @Volatile
        private var instance: CalcDatabase? = null

        fun build(context: Context): CalcDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalcDatabase::class.java,
                    "calc_database"
                ).build().also { instance = it }
            }
    }
}
