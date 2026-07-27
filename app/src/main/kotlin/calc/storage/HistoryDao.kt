package calc.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import calc.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [HistoryEntry], per CALC-DATA-01 §2.3.
 *
 * No deleteAll() — deliberate, per §2.3: "decided: no bulk 'clear all
 * history' action. Deletion is per-entry via swipe only."
 */
@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entry: HistoryEntry)

    /**
     * Reactive stream ordered by timestamp DESC, for the History screen
     * (§2.3). Not a suspend fun — Flow already handles asynchrony/
     * reactivity on its own.
     */
    @Query("SELECT * FROM history_entry ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntry>>

    /** Swipe-to-delete, CALC-01 §8. */
    @Query("DELETE FROM history_entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}
