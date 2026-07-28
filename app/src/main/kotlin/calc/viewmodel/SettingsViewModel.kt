package calc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import calc.storage.SettingsRepository
import calc.storage.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns user preferences (theme, haptics, sound, high contrast) as
 * their own domain, per an explicit decision made when the Execution
 * Pulse animation needed a way to read `hapticsEnabled`: each
 * ViewModel should align with one coherent responsibility —
 * CalcViewModel (calculation), HistoryViewModel (history/pins),
 * MemoryViewModel (M1-M3), and now SettingsViewModel (preferences) —
 * rather than bolting settings access onto whichever ViewModel
 * happened to need one property first.
 *
 * Intentionally small right now: only hapticsEnabled is actually
 * consumed anywhere yet (by the Execution Pulse's completion haptic).
 * themeMode/soundEnabled/highContrastEnabled are exposed alongside it
 * because SettingsRepository already provides all four with identical
 * shape (CALC-DATA-01 §3.3) and a future Settings screen will need
 * all of them — wiring them together now avoids a near-identical
 * second edit to this file later, without expanding scope: nothing
 * currently reads themeMode/soundEnabled/highContrastEnabled from
 * this ViewModel, they're just available for whenever the Settings
 * screen batch needs them.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Default true: CALC-DATA-01 §3.3 / PreferencesManager's
            // own DataStore default for this key is true (confirmed
            // by reading PreferencesManager before writing this) — the
            // stateIn initialValue mirrors that same default rather
            // than guessing false, so there's no flash of
            // haptics-off before the first real DataStore emission
            // arrives.
            initialValue = true
        )

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Default DARK: matches ThemeMode.fromStored's fallback
            // in PreferencesManager.kt, per §3.3.
            initialValue = ThemeMode.DARK
        )

    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Default false: PreferencesManager's DataStore default
            // for this key is false, per §3.3 (confirmed by reading
            // PreferencesManager directly — don't assume this matches
            // hapticsEnabled's default, they differ).
            initialValue = false
        )

    val highContrastEnabled: StateFlow<Boolean> = settingsRepository.highContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Default false, per PreferencesManager.kt / §3.3.
            initialValue = false
        )

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHighContrastEnabled(enabled) }
    }
}
