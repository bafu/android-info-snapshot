package io.numbers.infosnapshot.factories

import android.content.Context
import android.util.Log
import com.google.android.gms.location.*
import io.numbers.infosnapshot.model.info.LocationData
import io.numbers.infosnapshot.model.info.LocationInfo
import io.numbers.infosnapshot.utils.NullReason
import io.numbers.infosnapshot.utils.NullableWithReason
import io.numbers.infosnapshot.utils.TAG
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

object LocationInfoFactory {

    suspend fun newLocationInfo(context: Context, duration: Long) = coroutineScope {
        val locationRequest = createLocationRequest(duration)
        val client = LocationServices.getFusedLocationProviderClient(context)

        var lastKnownLocationData =
            NullableWithReason<LocationData>(NullReason.SNAP_DURATION_TOO_SHORT)

        client.lastLocation.addOnSuccessListener {
            lastKnownLocationData = NullableWithReason(LocationData.fromLocation(it))
        }

        val currentLocations = mutableListOf<LocationData>()
        val locationCallback = createLocationCallback(currentLocations)
        try {
            client.requestLocationUpdates(locationRequest, locationCallback, null)
            delay(duration)
        } finally {
            client.removeLocationUpdates(locationCallback)
        }

        val lastCurrentLocation: NullableWithReason<LocationData> =
            if (currentLocations.isEmpty()) NullableWithReason(NullReason.SNAP_DURATION_TOO_SHORT)
            else NullableWithReason(currentLocations.last())

        return@coroutineScope LocationInfo(
            lastKnownLocationData,
            lastCurrentLocation
        )
    }

    private fun createLocationCallback(locationHolder: MutableList<LocationData>) =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                super.onLocationResult(result)
                result?.apply {
                    locationHolder.add(LocationData.fromLocation(lastLocation))
                } ?: Log.e(TAG, "Null result from location callback.")
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                super.onLocationAvailability(availability)
                Log.i(TAG, "Location availability: ${availability.isLocationAvailable}")
            }
        }

    private fun createLocationRequest(duration: Long) = LocationRequest.create().apply {
        interval = duration / 2
        fastestInterval = duration / 5
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
}