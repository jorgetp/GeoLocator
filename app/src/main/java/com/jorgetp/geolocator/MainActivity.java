package com.jorgetp.geolocator;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jorgetp.geolocator.adapter.CardsAdapter;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String NOTIFICATION_CHANNEL_ID = "location_notifications";

    private FusedLocationProviderClient fusedLocationClient;
    private double lat = 0.0;
    private double lng = 0.0;
    private String address = "Unknown";
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

    private void refreshUI() {
        RecyclerView rvCards = findViewById(R.id.rv_cards);
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvCards.setAdapter(new CardsAdapter(this, lat, lng, address));
        rvCards.setVisibility(View.VISIBLE);

        setTitle(getString(R.string.current_location));
        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.GONE);

        TextView tvMsg = findViewById(R.id.tv_msg);
        tvMsg.setVisibility(View.VISIBLE);

        if (shouldSendNotification && !address.isEmpty() && !"Unknown".equals(address)) {
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