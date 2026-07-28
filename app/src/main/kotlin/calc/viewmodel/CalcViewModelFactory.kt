package calc.viewmodel

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Builds a ViewModelProvider.Factory wiring all three ViewModels
 * (CalcViewModel, HistoryViewModel, MemoryViewModel) to their
 * repositories via the given AppContainer.
 *
 * Uses the current androidx.lifecycle viewModelFactory DSL
 * (lifecycle-viewmodel, confirmed current as of this project's pinned
 * 2.10.0) rather than a hand-written ViewModelProvider.Factory class
 * — simpler for this project's small, fixed set of ViewModels, and
 * avoids CreationExtras.Key boilerplate since none of these
 * ViewModels need anything from CreationExtras itself (no
 * SavedStateHandle, no Application) — everything they need comes from
 * the closure over `container` below.
 *
 * Usage (from MainActivity):
 *   val container = AppContainer(applicationContext)
 *   setContent {
 *       val calcViewModel: CalcViewModel = viewModel(factory = calcViewModelFactory(container))
 *   }
 */
fun calcViewModelFactory(container: AppContainer) = viewModelFactory {
    initializer {
        CalcViewModel(historyRepository = container.historyRepository)
    }
    initializer {
        HistoryViewModel(
            historyRepository = container.historyRepository,
            pinnedRepository = container.pinnedRepository
        )
    }
    initializer {
        MemoryViewModel(settingsRepository = container.settingsRepository)
    }
}
