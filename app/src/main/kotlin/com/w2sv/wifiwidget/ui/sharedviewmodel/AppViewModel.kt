package com.w2sv.wifiwidget.ui.sharedviewmodel

import android.location.LocationManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.WidgetConfigurationScreenDestination
import com.ramcosta.composedestinations.spec.Direction
import com.w2sv.common.AppExtra
import com.w2sv.domain.model.Theme
import com.w2sv.domain.repository.PermissionRepository
import com.w2sv.domain.repository.PreferencesRepository
import com.w2sv.wifiwidget.di.MakeSnackbarVisuals
import com.w2sv.wifiwidget.di.MutableMakeSnackbarVisualsFlow
import com.w2sv.wifiwidget.ui.utils.direction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    @MutableMakeSnackbarVisualsFlow makeSnackbarVisualsFlow: MutableSharedFlow<MakeSnackbarVisuals>,
    private val permissionRepository: PermissionRepository,
    private val preferencesRepository: PreferencesRepository,
    val locationManager: LocationManager,
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    val startDirection: Direction = if (savedStateHandle.get<Boolean>(AppExtra.INVOKE_WIDGET_CONFIGURATION_SCREEN) == true) {
        WidgetConfigurationScreenDestination
    } else {
        HomeScreenDestination
    }
        .direction

    val makeSnackbarVisualsFlow = makeSnackbarVisualsFlow.asSharedFlow()

    val theme = preferencesRepository.inAppTheme.stateIn(
        viewModelScope,
        SharingStarted.Eagerly
    )

    fun saveTheme(theme: Theme) {
        viewModelScope.launch {
            preferencesRepository.inAppTheme.save(theme)
        }
    }

    val useDynamicColors = preferencesRepository.useDynamicTheme.stateIn(
        viewModelScope,
        SharingStarted.Eagerly
    )

    fun saveUseDynamicColors(value: Boolean) {
        viewModelScope.launch {
            preferencesRepository.useDynamicTheme.save(value)
        }
    }

    val useAmoledBlackTheme = preferencesRepository.useAmoledBlackTheme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed()
    )

    fun saveUseAmoledBlackTheme(value: Boolean) {
        viewModelScope.launch {
            preferencesRepository.useAmoledBlackTheme.save(value)
        }
    }

    // ===============
    // Permissions
    // ===============

    val locationAccessPermissionRequested by permissionRepository::locationAccessPermissionRequested

    fun saveLocationAccessPermissionRequested() {
        viewModelScope.launch { permissionRepository.locationAccessPermissionRequested.save(true) }
    }

    val locationAccessRationalShown by permissionRepository::locationAccessPermissionRationalShown

    fun saveLocationAccessRationalShown() {
        viewModelScope.launch { permissionRepository.locationAccessPermissionRationalShown.save(true) }
    }
}
