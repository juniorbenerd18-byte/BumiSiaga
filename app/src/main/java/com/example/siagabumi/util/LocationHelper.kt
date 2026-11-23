package com.example.siagabumi.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class GpsLocationResult(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)

@Singleton
class LocationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation(): GpsLocationResult? {
        if (!hasLocationPermission()) return null

        val location = getBestLocation() ?: return null
        val locationName = reverseGeocode(location.latitude, location.longitude)

        return GpsLocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = locationName
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun getBestLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // 1. Check last known location first (quickest)
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (_: SecurityException) { }
        }

        val fifteenMinutesAgo = System.currentTimeMillis() - 15 * 60 * 1000
        if (bestLocation != null && bestLocation.time > fifteenMinutesAgo) {
            return bestLocation
        }

        // 2. Request a fresh location with 6 second timeout
        val freshLocation = withTimeoutOrNull(6000L) {
            requestFreshLocation(lm)
        }

        return freshLocation ?: bestLocation
    }

    private suspend fun requestFreshLocation(lm: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                val provider = when {
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (provider != null) {
                    lm.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                    cont.invokeOnCancellation {
                        lm.removeUpdates(listener)
                    }
                } else {
                    cont.resume(null)
                }
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext formatCoordFallback(latitude, longitude)
            }

            val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocodeApi33(geocoder, latitude, longitude)
            } else {
                geocodeLegacy(geocoder, latitude, longitude)
            }

            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val cityOrKab = addr.subAdminArea ?: addr.locality
                val prov = addr.adminArea
                when {
                    !cityOrKab.isNullOrBlank() && !prov.isNullOrBlank() -> "$cityOrKab, $prov"
                    !cityOrKab.isNullOrBlank() -> cityOrKab
                    !prov.isNullOrBlank() -> prov
                    else -> formatCoordFallback(latitude, longitude)
                }
            } else {
                formatCoordFallback(latitude, longitude)
            }
        } catch (_: Exception) {
            formatCoordFallback(latitude, longitude)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun geocodeApi33(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): List<Address>? = suspendCancellableCoroutine { cont ->
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                if (cont.isActive) cont.resume(addresses)
            }

            override fun onError(errorMessage: String?) {
                if (cont.isActive) cont.resume(null)
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun geocodeLegacy(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): List<Address>? {
        return geocoder.getFromLocation(latitude, longitude, 1)
    }

    private fun formatCoordFallback(latitude: Double, longitude: Double): String {
        return "Lokasi GPS (${String.format(Locale.US, "%.2f", latitude)}, ${String.format(Locale.US, "%.2f", longitude)})"
    }
}
