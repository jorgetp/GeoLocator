package com.jorgetp.geolocator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jorgetp.geolocator.adapter.ItemsAdapter;

public class MainActivity extends AppCompatActivity {
    public static final String NOTIFICATION_CHANNEL_ID = "geo_locator_channel";

    private double currentLat, currentLng;
    private String currentAddress;
    private boolean locationLoaded = false;
    private ItemsAdapter adapter;

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

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        createNotificationChannel();

        refreshCurrentLocation();

        FloatingActionButton fabRefresh = findViewById(R.id.fab_refresh);
        fabRefresh.setOnClickListener(v -> refreshCurrentLocation());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.menu_save);
        // Enable save menu only when location is loaded
        saveItem.setEnabled(locationLoaded);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) {
            if (locationLoaded)
                adapter.addItem(currentLat, currentLng, currentAddress);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Location Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void refreshCurrentLocation() {
        locationLoaded = false;
        invalidateOptionsMenu(); // This will call onPrepareOptionsMenu to disable save button

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.determining_location));

        RecyclerView rvItems = findViewById(R.id.rv_items);
        rvItems.setVisibility(View.GONE);

        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.VISIBLE);

        LocationWorker.handleLocation(
                this,
                (lat, lon, address) -> runOnUiThread(() ->
                        onLocationLoaded(lat, lon, address, toolbar, rvItems, progressBar))
        );
    }

    private void onLocationLoaded(double lat, double lon, String address,
                                  Toolbar toolbar, RecyclerView rvItems, ProgressBar progressBar) {
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter = new ItemsAdapter(this, lat, lon, address));
        rvItems.setVisibility(View.VISIBLE);

        toolbar.setTitle(getString(R.string.current_location));
        progressBar.setVisibility(View.GONE);

        currentLat = lat;
        currentLng = lon;
        currentAddress = address;
        locationLoaded = true;

        invalidateOptionsMenu(); // This will enable the save button
    }
}