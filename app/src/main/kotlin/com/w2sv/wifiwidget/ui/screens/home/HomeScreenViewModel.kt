package com.w2sv.wifiwidget.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w2sv.domain.model.WifiProperty
import com.w2sv.domain.model.WifiStatus
import com.w2sv.domain.repository.WidgetRepository
import com.w2sv.kotlinutils.coroutines.collectLatestFromFlow
import com.w2sv.kotlinutils.coroutines.valueEnabledKeys
import com.w2sv.networking.WifiStatusMonitor
import com.w2sv.wifiwidget.ui.screens.home.components.wifistatus.model.WifiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import slimber.log.i
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    widgetRepository: WidgetRepository,
    wifiStatusMonitor: WifiStatusMonitor,
    wifiPropertyViewDataFactory: WifiProperty.ViewData.Factory,
) : ViewModel() {

    private val wifiStateEmitter = WifiStateEmitter(
        wifiPropertyEnablementMap = widgetRepository.wifiPropertyEnablementMap.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            getDefault = { false }
        ),
        ipSubPropertyEnablementMap = widgetRepository.ipSubPropertyEnablementMap.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            getDefault = { false }
        ),
        wifiStatusFlow = wifiStatusMonitor.wifiStatus,
        wifiPropertyViewDataFactory = wifiPropertyViewDataFactory,
        scope = viewModelScope
    )

    val wifiState by wifiStateEmitter::state

    fun refreshPropertyViewDataIfConnected() {
        wifiStateEmitter.refreshPropertyViewDataIfConnected()
    }
}

private class WifiStateEmitter(
    private val wifiPropertyEnablementMap: Map<WifiProperty, StateFlow<Boolean>>,
    private val ipSubPropertyEnablementMap: Map<WifiProperty.IP.SubProperty, StateFlow<Boolean>>,
    private val wifiStatusFlow: Flow<WifiStatus>,
    private val wifiPropertyViewDataFactory: WifiProperty.ViewData.Factory,
    scope: CoroutineScope
) {
    val state: StateFlow<WifiState> get() = _state.asStateFlow()
    private val _state = MutableStateFlow<WifiState>(WifiState.Disconnected)

    private fun getPropertyViewData(): Flow<WifiProperty.ViewData> =
        wifiPropertyViewDataFactory(
            properties = wifiPropertyEnablementMap.valueEnabledKeys(),
            ipSubProperties = ipSubPropertyEnablementMap.valueEnabledKeys().toSet(),
        )

    private fun setState(wifiStatus: WifiStatus) {
        _state.value = when (wifiStatus) {
            WifiStatus.Disabled -> WifiState.Disabled
            WifiStatus.Disconnected -> WifiState.Disconnected
            WifiStatus.Connected -> WifiState.Connected(
                propertyViewData = getPropertyViewData()
            )
        }
            .also {
                i { "Set wifiState=$it" }
            }
    }

    fun refreshPropertyViewDataIfConnected() {
        if (state.value is WifiState.Connected) {
            _state.value = WifiState.Connected(getPropertyViewData())
        }
    }

    init {
        with(scope) {
            collectLatestFromFlow(wifiStatusFlow) { status ->
                i { "Collected WifiStatus=$status" }
                setState(status)
            }
            collectLatestFromFlow(
                (wifiPropertyEnablementMap.values + ipSubPropertyEnablementMap.values)
                    .merge()
            ) {
                i { "Refreshing on property enablement change" }
                refreshPropertyViewDataIfConnected()
            }
        }
    }
}