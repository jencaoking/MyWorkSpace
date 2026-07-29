package com.jencao.mywork.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/** 封装系统定位，优先 GPS 最后已知位置，必要时请求单次更新（带超时）。 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun getLastLocation(): Location? {
        if (!hasPermission()) return null
        val gps = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val net = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        return gps ?: net
    }

    @SuppressLint("MissingPermission")
    suspend fun requestCurrentLocation(timeoutMs: Long = 10000): Location? = suspendCancellableCoroutine { cont ->
        if (!hasPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val last = getLastLocation()
        if (last != null) {
            cont.resume(last)
            return@suspendCancellableCoroutine
        }
        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (cont.isActive) cont.resume(loc)
                runCatching { lm.removeUpdates(this) }
            }
        }
        try {
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (cont.isActive) {
                cont.resume(null)
                runCatching { lm.removeUpdates(listener) }
            }
        }, timeoutMs)
    }
}
