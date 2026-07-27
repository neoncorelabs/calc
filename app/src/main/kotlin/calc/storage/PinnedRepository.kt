package calc.storage

import calc.model.PinnedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Tier 2 (cold path) wrapper over [PinnedDao], per CALC-ARCH-01 §4/§5
 * and CALC-DATA-01 §2.2 (CALC-01 §7: long-press result -> Pin/Save).
 * The ViewModel talks to this, never to PinnedDao/CalcDatabase
 * directly.
 *
 * Same assembly pattern as HistoryRepository: insert() takes the raw
 * fields and builds the full PinnedResult here, including pinnedAt,
 * so callers never need to know the entity's shape. label is
 * nullable, matching PinnedResult.label (§2.2) — a pin can be created
 * without a user-given name.
 */
class PinnedRepository(private val pinnedDao: PinnedDao) {

    val pinned: Flow<List<PinnedResult>> = pinnedDao.getAll()

    suspend fun insert(label: String?, expression: String, result: String, resultRaw: Double) {
        withContext(Dispatchers.IO) {
            pinnedDao.insert(
                PinnedResult(
                    label = label,
                    expression = expression,
                    result = result,
                    resultRaw = resultRaw,
                    pinnedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteById(id: Long) {
        withContext(Dispatchers.IO) {
            pinnedDao.deleteById(id)
        }
    }

    suspend fun updateLabel(id: Long, label: String?) {
        withContext(Dispatchers.IO) {
            pinnedDao.updateLabel(id, label)
        }
    }
}
