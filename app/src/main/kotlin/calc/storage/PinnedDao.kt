package calc.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import calc.model.PinnedResult
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [PinnedResult], per CALC-DATA-01 §2.3: insert, getAll,
 * deleteById, updateLabel(id, label).
 */
@Dao
interface PinnedDao {

    @Insert
    suspend fun insert(pinned: PinnedResult)

    @Query("SELECT * FROM pinned_result")
    fun getAll(): Flow<List<PinnedResult>>

    @Query("DELETE FROM pinned_result WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pinned_result SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String?)
}
