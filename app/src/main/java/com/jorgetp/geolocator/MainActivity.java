package com.jorgetp.geolocator;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private double lat = 0.0;
    private double lng = 0.0;
    private String address = "Unknown";
    private String plusCode = "Unknown";

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

        getLocation();

        FloatingActionButton fabRefresh = findViewById(R.id.fab_refresh);
        fabRefresh.setOnClickListener(v -> getLocation());
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        setTitle("Determining location...");

        TextView tvMsg = findViewById(R.id.tv_msg);
        tvMsg.setVisibility(View.GONE);

        RecyclerView rvCards = findViewById(R.id.rv_cards);
        rvCards.setVisibility(View.GONE);

        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getCurrentLocation(
                        LocationRequest.PRIORITY_HIGH_ACCURACY,
                        null
                ).addOnSuccessListener(this, location -> {
                    if (location != null) {
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                        getAddress(location, true);
                        getPlusCode(true);
                        Log.d("Location", "Latitude: " + lat + ", Longitude: " + lng);
                    } else {
                        Log.w("Location", "Location is null.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Location", "Failed to get location", e));
    }

    private void getAddress(Location location, boolean refreshUI) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1 // max results
            );

            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
                if (refreshUI) refreshUI();
                Log.d("Address", "Address: " + address);
            } else {
                Log.w("Address", "No address found.");
            }
        } catch (IOException e) {
            Log.e("Address", "Geocoder failed", e);
        }
    }

    private void getPlusCode(boolean refreshUI) {
        new Thread(() -> {
            SharedPreferences sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            String apiKey = sharedPreferences.getString("api_key", "");
            String addressSource = sharedPreferences.getString("address_source", "android");
            try {
                // Get address using Geocoding API
                String geocodeUrlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + lat + "," + lng + "&key=" + apiKey;
                URL geocodeUrl = new URL(geocodeUrlStr);
                HttpURLConnection geocodeConn = (HttpURLConnection) geocodeUrl.openConnection();
                geocodeConn.setRequestMethod("GET");
                BufferedReader geocodeReader = new BufferedReader(new InputStreamReader(geocodeConn.getInputStream()));
                String line;
                StringBuilder geocodeResponse = new StringBuilder();
                while ((line = geocodeReader.readLine()) != null) {
                    geocodeResponse.append(line);
                }
                geocodeReader.close();
                JSONObject geocodeJson = new JSONObject(geocodeResponse.toString());
                if (addressSource.equals("maps")) {
                    JSONArray results = geocodeJson.getJSONArray("results");
                    if (results.length() > 0) {
                        address = results.getJSONObject(0).getString("formatted_address");
                    } else {
                        address = "Unknown";
                    }
                }

                // Get Plus Code from geocode response if available
                JSONObject plusCodeObj = geocodeJson.optJSONObject("plus_code");
                if (plusCodeObj != null) {
                    if (plusCodeObj.has("compound_code")) {
                        plusCode = plusCodeObj.optString("compound_code", "Unknown");
                    } else if (plusCodeObj.has("global_code")) {
                        plusCode = plusCodeObj.optString("global_code", "Unknown");
                    } else {
                        plusCode = "Unknown";
                    }
                } else {
                    plusCode = "Unknown";
                }

            } catch (Exception e) {
                address = "Unable to get address";
                plusCode = "Unable to get Plus Code";
            }

            if (refreshUI)
                runOnUiThread(this::refreshUI);

        }).start();
    }

    private void refreshUI() {
        RecyclerView rvCards = findViewById(R.id.rv_cards);
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvCards.setAdapter(new CardsAdapter(this, lat, lng, address, plusCode));
        rvCards.setVisibility(View.VISIBLE);

        setTitle("Your location");
        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.GONE);

        TextView tvMsg = findViewById(R.id.tv_msg);
        tvMsg.setVisibility(View.VISIBLE);
    }
}