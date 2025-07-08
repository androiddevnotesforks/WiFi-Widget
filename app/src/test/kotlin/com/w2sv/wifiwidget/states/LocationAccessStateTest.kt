package com.w2sv.wifiwidget.states

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.w2sv.wifiwidget.ui.screens.home.components.LocationAccessPermissionStatus
import com.w2sv.wifiwidget.ui.states.LocationAccessState
import com.w2sv.wifiwidget.ui.utils.ScopedSnackbarEmitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock

class LocationAccessStateTest {

    @Test
    fun `newStatus emits on multiplePermissionsState allPermissionsGranted changes`() =
        runTest {
            val multiplePermissionsState = MultiplePermissionsStateTestImpl()

            locationAccessState(multiplePermissionsState).newStatus.test {
                Snapshot.withMutableSnapshot { multiplePermissionsState.allPermissionsGrantedState.value = true }
                assertEquals(LocationAccessPermissionStatus.Granted(null), awaitItem())

                Snapshot.withMutableSnapshot { multiplePermissionsState.allPermissionsGrantedState.value = false }
                assertEquals(LocationAccessPermissionStatus.NotGranted, awaitItem())
            }
        }

    @Test
    fun `saveRequestLaunchedBefore called on requestResult emission while requestLaunchedBefore false and not called if true`() =
        runTest {
            val requestResult = MutableSharedFlow<Boolean>()
            val requestLaunchedBefore = MutableStateFlow(false)
            var saveRequestLaunchedBeforeCallCount = 0

            locationAccessState(
                requestResult = requestResult,
                requestLaunchedBefore = requestLaunchedBefore,
                saveRequestLaunchedBefore = { saveRequestLaunchedBeforeCallCount += 1 }
            )

            requestResult.emit(false)
            assertEquals(1, saveRequestLaunchedBeforeCallCount)

            requestLaunchedBefore.value = true
            requestResult.emit(false)
            assertEquals(1, saveRequestLaunchedBeforeCallCount)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun locationAccessState(
    permissionsState: MultiplePermissionsStateTestImpl = MultiplePermissionsStateTestImpl(),
    requestResult: MutableSharedFlow<Boolean> = MutableSharedFlow(),
    requestLaunchedBefore: MutableStateFlow<Boolean> = MutableStateFlow(false),
    saveRequestLaunchedBefore: () -> Unit = {},
    rationalShown: MutableStateFlow<Boolean> = MutableStateFlow(false),
    saveRationalShown: () -> Unit = {},
    scope: CoroutineScope = TestScope(UnconfinedTestDispatcher())
): LocationAccessState =
    LocationAccessState(
        permissionsState = permissionsState,
        requestResult = requestResult,
        scope = scope,
        backgroundAccessState = null,
        requestLaunchedBefore = requestLaunchedBefore,
        saveRequestLaunchedBefore = saveRequestLaunchedBefore,
        rationalShown = rationalShown,
        saveRationalShown = saveRationalShown,
        snackbarEmitter = ScopedSnackbarEmitter(SnackbarHostState(), mock(), scope),
        openAppSettings = {}
    )

private class MultiplePermissionsStateTestImpl : MultiplePermissionsState {
    override val permissions: List<PermissionState> = emptyList()
    override val revokedPermissions: List<PermissionState> = emptyList()

    val allPermissionsGrantedState = mutableStateOf(false)
    override val allPermissionsGranted: Boolean get() = allPermissionsGrantedState.value

    val shouldShowRationaleState = mutableStateOf(false)
    override val shouldShowRationale: Boolean get() = shouldShowRationaleState.value

    override fun launchMultiplePermissionRequest() {}
}
