package calc.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore extension property. Single instance across the app via
 * property delegation, per standard DataStore usage pattern.
 * File name "calc_prefs" matches CALC-DATA-01 §5's schema diagram.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calc_prefs")

/**
 * Theme mode values per CALC-DATA-01 §3.3. Modeled as a String-backed
 * enum since the underlying preference is stored as a String (matching
 * the spec's literal type column), but exposed as a typed enum at the
 * Kotlin API boundary rather than raw strings.
 */
enum class ThemeMode {
    DARK, LIGHT, SYSTEM;

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.find { it.name == value } ?: DARK // default per §3.3
    }
}

/**
 * Wraps DataStore (Preferences) access for memory registers (§3.1) and
 * app settings (§3.3). §3.2 (engine state / angle_mode) is deliberately
 * absent — the spec explicitly retains that section only as a
 * placeholder with no fields to implement in v1.
 *
 * Memory registers are modeled as Double? in the public API to match
 * §3.1's stated type and "null = empty slot" semantics, even though
 * DataStore's Preferences storage has no native nullable-double key —
 * absence of the key IS the null/empty-slot state; a present key
 * holds a real Double.
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val MEMORY_M1 = doublePreferencesKey("memory_m1")
        val MEMORY_M2 = doublePreferencesKey("memory_m2")
        val MEMORY_M3 = doublePreferencesKey("memory_m3")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HIGH_CONTRAST_ENABLED = booleanPreferencesKey("high_contrast_enabled")
    }

    // ---- Memory registers (§3.1) ----

    val memoryM1: Flow<Double?> = context.dataStore.data.map { it[Keys.MEMORY_M1] }
    val memoryM2: Flow<Double?> = context.dataStore.data.map { it[Keys.MEMORY_M2] }
    val memoryM3: Flow<Double?> = context.dataStore.data.map { it[Keys.MEMORY_M3] }

    suspend fun setMemoryM1(value: Double?) = setOrClear(Keys.MEMORY_M1, value)
    suspend fun setMemoryM2(value: Double?) = setOrClear(Keys.MEMORY_M2, value)
    suspend fun setMemoryM3(value: Double?) = setOrClear(Keys.MEMORY_M3, value)

    private suspend fun setOrClear(key: Preferences.Key<Double>, value: Double?) {
        context.dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs[key] = value
            }
        }
    }

    // ---- App settings (§3.3) ----

    val themeMode: Flow<ThemeMode> =
        context.dataStore.data.map { ThemeMode.fromStored(it[Keys.THEME_MODE]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    val hapticsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true } // default true, §3.3

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HAPTICS_ENABLED] = enabled }
    }

    val soundEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: false } // default false, §3.3

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SOUND_ENABLED] = enabled }
    }

    val highContrastEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HIGH_CONTRAST_ENABLED] ?: false } // default false, §3.3

    suspend fun setHighContrastEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HIGH_CONTRAST_ENABLED] = enabled }
    }
}
