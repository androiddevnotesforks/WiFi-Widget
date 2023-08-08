package com.w2sv.data.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.w2sv.data.model.WifiStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class WifiStatusMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val connectivityManager = context.getConnectivityManager()
    private val wifiManager = context.getWifiManager()

    val wifiStatus: Flow<WifiStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                channel.trySend(WifiStatus.Connected)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (network == connectivityManager.activeNetwork) {
                    channel.trySend(WifiStatus.Connected)
                }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                if (network == connectivityManager.activeNetwork) {
                    channel.trySend(WifiStatus.Connected)
                }
            }

            override fun onUnavailable() {
                channel.trySend(WifiStatus.getNoConnectionPresentStatus(wifiManager))
            }

            override fun onLost(network: Network) {
                channel.trySend(WifiStatus.getNoConnectionPresentStatus(wifiManager))
            }
        }

        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build(),
            callback
        )

        channel.trySend(WifiStatus.get(context))

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}