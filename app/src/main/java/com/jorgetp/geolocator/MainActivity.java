package com.jorgetp.geolocator;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private double lat = 0.0;
    private double lng = 0.0;
    private String address = "Unknown Location";
    private String plusCode = "Unknown Plus Code";
    private String googleMapsAPIResponse = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        getCurrentLocation();

        Button buttonCopyCoordinates = findViewById(R.id.button_copy_coordinates);
        buttonCopyCoordinates.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Location Coordinates", lat + ", " + lng);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Copied coordinates: " + lat + ", " + lng);
        });

        Button buttonCopyAddress = findViewById(R.id.button_copy_address);
        buttonCopyAddress.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Location Address", address);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Copied address: " + address);
        });

        Button buttonCopyPlusCode = findViewById(R.id.button_copy_plus_code);
        buttonCopyPlusCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Location Plus Code", plusCode);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Copied Plus Code: " + plusCode);
        });

        Button refreshButton = findViewById(R.id.button_refresh);
        refreshButton.setOnClickListener(v -> {
            getCurrentLocation();
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LinearLayout linearLayoutLocationInfo = findViewById(R.id.linearLayout_location_info);
        linearLayoutLocationInfo.setVisibility(View.INVISIBLE);

        ProgressBar progressBar = findViewById(R.id.progressBar_loading);
        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                        Log.d("Location", "Latitude: " + lat + ", Longitude: " + lng);
                        getAddressFromLocation(location);
                        getPlusCode(true);
                    } else {
                        Log.w("Location", "Location is null.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Location", "Failed to get location", e));
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1 // max results
            );

            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
                Log.d("Address", "Address: " + address);
            } else {
                Log.w("Address", "No address found.");
            }
        } catch (IOException e) {
            Log.e("Address", "Geocoder failed", e);
        }
    }

    private void updateUI() {
        TextView textViewCoordinates = findViewById(R.id.textView_coordinates);
        textViewCoordinates.setText(String.format("%s, %s", lat, lng));

        TextView textViewAddress = findViewById(R.id.textView_address);
        textViewAddress.setText(address);

        TextView textViewPlusCode = findViewById(R.id.textView_plus_code);
        textViewPlusCode.setText(plusCode);
        //textViewPlusCode.setText(googleMapsAPIResponse);

        LinearLayout linearLayoutLocationInfo = findViewById(R.id.linearLayout_location_info);
        linearLayoutLocationInfo.setVisibility(View.VISIBLE);

        ProgressBar progressBar = findViewById(R.id.progressBar_loading);
        progressBar.setVisibility(View.GONE);
    }

    private void getPlusCode(boolean refreshUI) {
        new Thread(() -> {
            String apiKey = "...";
            final StringBuilder geocodeResponse = new StringBuilder();
            String line;
            try {
                /*// Prepare the HTTP POST request
                URL url = new URL("https://www.googleapis.com/geolocation/v1/geolocate?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                // Send empty JSON body
                OutputStream os = conn.getOutputStream();
                os.write("{}".getBytes(StandardCharsets.UTF_8));
                os.close();

                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                JSONObject json = new JSONObject(response.toString());
                JSONObject location = json.getJSONObject("location");
                lat = location.getDouble("lat");
                lng = location.getDouble("lng");*/

                // Get address using Geocoding API
                String geocodeUrlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + lat + "," + lng + "&key=" + apiKey;
                URL geocodeUrl = new URL(geocodeUrlStr);
                HttpURLConnection geocodeConn = (HttpURLConnection) geocodeUrl.openConnection();
                geocodeConn.setRequestMethod("GET");
                BufferedReader geocodeReader = new BufferedReader(new InputStreamReader(geocodeConn.getInputStream()));
                while ((line = geocodeReader.readLine()) != null) {
                    geocodeResponse.append(line);
                }
                geocodeReader.close();
                JSONObject geocodeJson = new JSONObject(geocodeResponse.toString());
                /*JSONArray results = geocodeJson.getJSONArray("results");
                if (results.length() > 0) {
                    address = results.getJSONObject(0).getString("formatted_address");
                } else {
                    address = "Unknown Location";
                }*/
                googleMapsAPIResponse = geocodeJson.toString();

                // Get Plus Code from geocode response if available
                JSONObject plusCodeObj = geocodeJson.optJSONObject("plus_code");
                if (plusCodeObj != null) {
                    if (plusCodeObj.has("compound_code")) {
                        plusCode = plusCodeObj.optString("compound_code", "Unknown Plus Code");
                    } else if (plusCodeObj.has("global_code")) {
                        plusCode = plusCodeObj.optString("global_code", "Unknown Plus Code");
                    } else {
                        plusCode = "Unknown Plus Code";
                    }
                } else {
                    plusCode = "Unknown Plus Code";
                }

            } catch (Exception e) {
                // address = "Unable to get address";
                plusCode = "Unable to get Plus Code";
            }

            if (refreshUI)
                runOnUiThread(this::updateUI);

        }).start();
    }
}