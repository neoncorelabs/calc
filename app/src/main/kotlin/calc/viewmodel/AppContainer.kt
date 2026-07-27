package calc.viewmodel

import android.content.Context
import calc.storage.CalcDatabase
import calc.storage.HistoryRepository
import calc.storage.PinnedRepository
import calc.storage.PreferencesManager
import calc.storage.SettingsRepository

/**
 * Manual DI container per CALC-ARCH-01 §7: no Hilt/Koin, a simple
 * object graph wired by hand. Spec rationale (§7): ~6 total
 * dependencies doesn't justify a DI framework's build-time and
 * startup cost.
 *
 * `context` must be an application Context (callers pass
 * `applicationContext`, not an Activity Context) since this object
 * is a process-lifetime singleton — holding an Activity Context here
 * would leak it.
 *
 * All properties are `by lazy`: nothing is constructed until first
 * used, matching §7's illustrative shape. Construction order below
 * follows the actual dependency chain (database/preferences first,
 * repositories after, since repositories wrap the DAOs/preferences).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // ---- Tier 2 foundations ----

    private val database: CalcDatabase by lazy {
        CalcDatabase.build(appContext)
    }

    private val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(appContext)
    }

    // ---- Tier 1 ----

    // EngineFacade is a stateless `object` (no per-instance state to
    // wire), so there's nothing to construct — CalcViewModel calls
    // calc.engine.EngineFacade directly. Listed here only in comments
    // for visibility against §7's dependency list.

    // ---- Tier 2 repositories ----

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(database.historyDao())
    }

    val pinnedRepository: PinnedRepository by lazy {
        PinnedRepository(database.pinnedDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(preferencesManager)
    }
}
