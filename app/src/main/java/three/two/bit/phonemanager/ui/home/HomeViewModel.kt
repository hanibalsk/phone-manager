package three.two.bit.phonemanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import three.two.bit.phonemanager.data.preferences.PreferencesRepository
import javax.inject.Inject

/**
 * Story E2.1: HomeViewModel
 *
 * Manages secret mode state and provides toggle functionality
 * ACs: E2.1.1, E2.1.2, E2.1.3
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    /**
     * Secret mode state (AC E2.1.1)
     */
    val isSecretModeEnabled: StateFlow<Boolean> =
        preferencesRepository.isSecretModeEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    /**
     * Toggle secret mode (AC E2.1.2, E2.1.3)
     * Called by hidden gestures - no visible feedback
     */
    fun toggleSecretMode() {
        viewModelScope.launch {
            val currentState = isSecretModeEnabled.value
            preferencesRepository.setSecretModeEnabled(!currentState)
        }
    }
}
