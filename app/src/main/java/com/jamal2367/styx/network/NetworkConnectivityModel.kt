/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.network

import android.app.Application
import android.net.ConnectivityManager
import com.jamal2367.styx.rx.BroadcastReceiverObservable
import dagger.Reusable
import io.reactivex.Observable
import javax.inject.Inject

/**
 * A model that supplies network connectivity status updates.
 */
@Reusable
class NetworkConnectivityModel @Inject constructor(
    private val connectivityManager: ConnectivityManager,
    private val application: Application
) {

    /**
     * An infinite observable that emits a boolean value whenever the network condition changes.
     * Emitted value is true when the network is in the connected state, and it is false otherwise.
     */
    @Suppress("DEPRECATION")
    fun connectivity(): Observable<Boolean> = BroadcastReceiverObservable(
        NETWORK_BROADCAST_ACTION,
        application
    ).map { connectivityManager.activeNetworkInfo?.isConnected == true }

    companion object {
        private const val NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    }

}
