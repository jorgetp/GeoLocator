package com.jorgetp.geolocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jorgetp.geolocator.adapter.CardsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
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
    private static final String NOTIFICATION_CHANNEL_ID = "location_notifications";

    private FusedLocationProviderClient fusedLocationClient;
    private double lat = 0.0;
    private double lng = 0.0;
    private String address = "Unknown";
    private String plusCode = "Unknown";
    private boolean shouldSendNotification = true;

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

        createNotificationChannel();

        getLocation();

        FloatingActionButton fabRefresh = findViewById(R.id.fab_refresh);
        fabRefresh.setOnClickListener(v -> getLocation());
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
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
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        setTitle(getString(R.string.determining_location));

        TextView tvMsg = findViewById(R.id.tv_msg);
        tvMsg.setVisibility(View.GONE);

        RecyclerView rvCards = findViewById(R.id.rv_cards);
        rvCards.setVisibility(View.GONE);

        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(this, location -> {
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();

                getAddress(location);
                getPlusCode();
            }
        });
    }

    private void getAddress(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1 // max results
            );

            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
                refreshUI();
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private void getPlusCode() {
        new Thread(() -> {
            String apiKey = getApplicationContext()
                    .getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getString("api_key", "");
            try {
                JSONObject geocodeJson = getGeocodeJson(apiKey);
                if ("Unknown".equals(address)) {
                    // Get address using Geocoding API
                    JSONArray results = geocodeJson.getJSONArray("results");
                    if (results.length() > 0)
                        address = results.getJSONObject(0).getString("formatted_address");
                    else
                        address = "Unknown";
                }

                // Get Plus Code from geocode response if available
                JSONObject plusCodeObj = geocodeJson.optJSONObject("plus_code");
                if (plusCodeObj != null) {
                    if (plusCodeObj.has("compound_code"))
                        plusCode = plusCodeObj.optString("compound_code", "Unknown");
                    else if (plusCodeObj.has("global_code"))
                        plusCode = plusCodeObj.optString("global_code", "Unknown");
                    else
                        plusCode = "Unknown";
                } else
                    plusCode = "Unknown";

            } catch (Exception e) {
                // e.printStackTrace();
                address = "Unknown";
                plusCode = "Unknown";
            }

            runOnUiThread(this::refreshUI);

        }).start();
    }

    @NonNull
    private JSONObject getGeocodeJson(String apiKey) throws IOException, JSONException {
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
        return new JSONObject(geocodeResponse.toString());
    }

    private void refreshUI() {
        RecyclerView rvCards = findViewById(R.id.rv_cards);
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvCards.setAdapter(new CardsAdapter(this, lat, lng, address, plusCode));
        rvCards.setVisibility(View.VISIBLE);

        setTitle(getString(R.string.current_location));
        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.GONE);

        TextView tvMsg = findViewById(R.id.tv_msg);
        tvMsg.setVisibility(View.VISIBLE);

        if (shouldSendNotification) {
            sendLocationNotification();
            shouldSendNotification = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Location Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void sendLocationNotification() {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String locationText = !"Unknown".equals(address) ? address :
                String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", lat, lng);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.current_location))
                .setContentText(locationText);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(782617515, builder.build());

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }
}