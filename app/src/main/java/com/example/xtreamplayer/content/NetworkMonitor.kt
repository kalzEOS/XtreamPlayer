package com.example.xtreamplayer.content

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitors network connectivity to pause/resume sync operations
 */
class NetworkMonitor(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    private val _networkAvailable = MutableStateFlow(isNetworkAvailable())
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private var isMonitoring = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("Network available")
            _networkAvailable.value = true
        }

        override fun onLost(network: Network) {
            Timber.d("Network lost")
            _networkAvailable.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            val hasValidated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

            val available = hasInternet && hasValidated
            if (_networkAvailable.value != available) {
                Timber.d("Network capabilities changed: internet=$hasInternet, validated=$hasValidated")
                _networkAvailable.value = available
            }
        }
    }

    /**
     * Check if network is currently available
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Start monitoring network state
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Timber.d("Network monitoring already started")
            return
        }

        Timber.i("Starting network monitoring")

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
        isMonitoring = true

        // Update initial state
        _networkAvailable.value = isNetworkAvailable()
    }

    /**
     * Stop monitoring network state
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            Timber.d("Network monitoring not started")
            return
        }

        Timber.i("Stopping network monitoring")

        connectivityManager.unregisterNetworkCallback(networkCallback)
        isMonitoring = false
    }
}
