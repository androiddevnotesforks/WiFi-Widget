package com.w2sv.wifiwidget.ui

import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.w2sv.common.utils.log
import com.w2sv.composed.CollectFromFlow
import com.w2sv.domain.model.Theme
import com.w2sv.widget.WifiWidgetProvider
import com.w2sv.wifiwidget.ui.navigation.NavGraph
import com.w2sv.wifiwidget.ui.navigation.Screen
import com.w2sv.wifiwidget.ui.screens.home.components.TriggerWidgetDataRefresh
import com.w2sv.wifiwidget.ui.sharedviewmodel.AppViewModel
import com.w2sv.wifiwidget.ui.sharedviewmodel.WidgetViewModel
import com.w2sv.wifiwidget.ui.states.rememberLocationAccessState
import com.w2sv.wifiwidget.ui.theme.AppTheme
import com.w2sv.wifiwidget.ui.utils.activityViewModel
import slimber.log.i

@Composable
fun AppUI(
    initialScreen: Screen,
    setSystemBarStyles: (SystemBarStyle, SystemBarStyle) -> Unit,
    appVM: AppViewModel = activityViewModel(),
    widgetVM: WidgetViewModel = activityViewModel()
) {
    CompositionLocalProvider(
        LocalUseDarkTheme provides when (appVM.theme.collectAsStateWithLifecycle().value) {
            Theme.Light -> false
            Theme.Dark -> true
            Theme.Default -> isSystemInDarkTheme()
        }
    ) {
        AppTheme(
            useDynamicTheme = appVM.useDynamicColors.collectAsStateWithLifecycle().value,
            useAmoledBlackTheme = appVM.useAmoledBlackTheme.collectAsStateWithLifecycle().value,
            setSystemBarStyles = setSystemBarStyles
        ) {
            val locationAccessState = rememberLocationAccessState()
            val context = LocalContext.current

            // Trigger onGrantActions on location permission state having been newly granted
            CollectFromFlow(locationAccessState.newStatus) {
                it.grantedOrNull?.onGrantAction?.let { onGrantAction ->
                    i { "Collected onGrantAction=$onGrantAction" }
                    when (onGrantAction) {
                        TriggerWidgetDataRefresh -> WifiWidgetProvider.triggerDataRefresh(context)
                            .log { "Triggered widget data refresh upon LocationAccessPermissionStatus having been granted" }

                        else -> widgetVM.configuration.onLocationAccessPermissionGranted(onGrantAction)
                    }
                }
            }

            CompositionLocalProvider(
                LocalLocationManager provides appVM.locationManager,
                LocalLocationAccessState provides locationAccessState
            ) {
                NavGraph(initialScreen = initialScreen)
            }
        }
    }
}
