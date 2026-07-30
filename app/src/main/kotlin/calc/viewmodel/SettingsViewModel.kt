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
 * Owns user preferences (theme, high contrast) as their own domain —
 * one ViewModel per coherent responsibility, alongside CalcViewModel
 * (calculation), HistoryViewModel (history/pins), and MemoryViewModel
 * (M1-M3), rather than bolting settings access onto whichever
 * ViewModel happened to need one property first.
 *
 * Does NOT expose hapticsEnabled/soundEnabled. Explicit product
 * decision: CALC does not use sound or haptic feedback — visual
 * transitions (the Execution Pulse, live preview, status color) are
 * the calculator's only feedback channel. This is a CALC-specific UI
 * decision, not a change to the underlying settings infrastructure —
 * SettingsRepository/PreferencesManager still support
 * hapticsEnabled/soundEnabled unchanged (CALC-DATA-01 §3.3), in case
 * a future NeonCoreLabs app wants them; SettingsViewModel simply
 * doesn't surface them to CALC's UI. See CALC-UI-01 §15/§18's
 * revision notes for the full rationale.
 *
 * themeMode/highContrastEnabled remain — visual settings, unaffected
 * by the sound/haptics decision, and a future Settings screen will
 * still need them.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Default DARK: matches ThemeMode.fromStored's fallback
            // in PreferencesManager.kt, per §3.3.
            initialValue = ThemeMode.DARK
        )

    val highContrastEnabled: StateFlow<Boolean> = settingsRepository.highContrastEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Default false, per PreferencesManager.kt / §3.3.
            initialValue = false
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHighContrastEnabled(enabled) }
    }
}
