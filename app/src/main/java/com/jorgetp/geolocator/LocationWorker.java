package com.jorgetp.geolocator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

public class LocationWorker {

    @SuppressLint("MissingPermission")
    public static void handleLocation(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();
        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(appContext);

        client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                String address = "Unknown";

                try {
                    Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty())
                        address = addresses.get(0).getAddressLine(0);

                } catch (Exception e) {
                    // e.printStackTrace();
                }

                // Notify MainActivity (or whoever) that the work is done
                if (callback != null)
                    callback.onLocationReady(lat, lon, address);

            }
        });
    }

    public interface Callback {
        void onLocationReady(double lat, double lon, String address);
    }
}
