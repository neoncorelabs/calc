package calc.storage

import calc.model.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Tier 2 (cold path) wrapper over [HistoryDao], per CALC-ARCH-01 §4/§5.
 * The ViewModel talks to this, never to HistoryDao/CalcDatabase
 * directly.
 *
 * insert() takes the raw expression + result pair (matching §4's
 * illustrative `historyRepository.insert(currentExpression, result)`
 * shape) and assembles the full [HistoryEntry] here — generating the
 * timestamp and bundling both the formatted display string and the
 * full-precision raw value, per CALC-DATA-01 §2.1/§2.1.1. The
 * ViewModel/CalcViewModel never needs to know HistoryEntry's shape.
 *
 * Per CALC-DATA-01 §6 item 2 (resolved): errors are never written to
 * history at all. This repository has no way to insert an error state —
 * callers only ever provide a successful expression/result/resultRaw
 * triple, which keeps that invariant enforced by the type signature
 * rather than by caller discipline.
 */
class HistoryRepository(private val historyDao: HistoryDao) {

    val history: Flow<List<HistoryEntry>> = historyDao.getAll()

    suspend fun insert(expression: String, result: String, resultRaw: Double) {
        withContext(Dispatchers.IO) {
            historyDao.insert(
                HistoryEntry(
                    expression = expression,
                    result = result,
                    resultRaw = resultRaw,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteById(id: Long) {
        withContext(Dispatchers.IO) {
            historyDao.deleteById(id)
        }
    }
}
