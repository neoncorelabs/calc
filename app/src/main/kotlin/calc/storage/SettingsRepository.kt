package calc.storage

import kotlinx.coroutines.flow.Flow

/**
 * Tier 2 (cold path) wrapper over [PreferencesManager], per
 * CALC-ARCH-01 §5's state ownership table: "Memory M1-M3" and
 * "theme, haptics/sound toggles" are both explicitly owned via
 * SettingsRepository (backed by DataStore), not read directly from
 * PreferencesManager by ViewModels.
 *
 * This is a thinner pass-through than HistoryRepository/
 * PinnedRepository since PreferencesManager already exposes
 * Flow/suspend-shaped access directly (§3's keys map cleanly to
 * typed properties with no entity-assembly step needed) — but it's
 * kept as a distinct class so MemoryViewModel and any future settings
 * UI depend on the same one-layer-of-indirection pattern as the
 * Room-backed repositories, per §5's explicit table.
 */
class SettingsRepository(private val preferencesManager: PreferencesManager) {

    // ---- Memory registers (CALC-DATA-01 §3.1) ----

    val memoryM1: Flow<Double?> = preferencesManager.memoryM1
    val memoryM2: Flow<Double?> = preferencesManager.memoryM2
    val memoryM3: Flow<Double?> = preferencesManager.memoryM3

    suspend fun setMemoryM1(value: Double?) = preferencesManager.setMemoryM1(value)
    suspend fun setMemoryM2(value: Double?) = preferencesManager.setMemoryM2(value)
    suspend fun setMemoryM3(value: Double?) = preferencesManager.setMemoryM3(value)

    // ---- App settings (CALC-DATA-01 §3.3) ----

    val themeMode: Flow<ThemeMode> = preferencesManager.themeMode
    suspend fun setThemeMode(mode: ThemeMode) = preferencesManager.setThemeMode(mode)

    val hapticsEnabled: Flow<Boolean> = preferencesManager.hapticsEnabled
    suspend fun setHapticsEnabled(enabled: Boolean) = preferencesManager.setHapticsEnabled(enabled)

    val soundEnabled: Flow<Boolean> = preferencesManager.soundEnabled
    suspend fun setSoundEnabled(enabled: Boolean) = preferencesManager.setSoundEnabled(enabled)

    val highContrastEnabled: Flow<Boolean> = preferencesManager.highContrastEnabled
    suspend fun setHighContrastEnabled(enabled: Boolean) =
        preferencesManager.setHighContrastEnabled(enabled)
}
