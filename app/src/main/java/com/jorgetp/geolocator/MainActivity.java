package com.jorgetp.geolocator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jorgetp.geolocator.adapter.CardsAdapter;

public class MainActivity extends AppCompatActivity {
    public static final String NOTIFICATION_CHANNEL_ID = "geo_locator_channel";

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

        createNotificationChannel();

        refreshLocation();

        FloatingActionButton fabRefresh = findViewById(R.id.fab_refresh);
        fabRefresh.setOnClickListener(v -> refreshLocation());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Location Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void refreshLocation() {
        setTitle(getString(R.string.determining_location));

        TextView tvMsg = findViewById(R.id.tv_msg);
        tvMsg.setVisibility(View.GONE);

        RecyclerView rvCards = findViewById(R.id.rv_cards);
        rvCards.setVisibility(View.GONE);

        ProgressBar progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.VISIBLE);

        LocationWorker.handleLocation(
                this,
                false,   // do NOT send notification when UI loads
                (lat, lon, address) -> runOnUiThread(() -> {
                    rvCards.setLayoutManager(new LinearLayoutManager(this,
                            LinearLayoutManager.VERTICAL, false));
                    rvCards.setAdapter(new CardsAdapter(this, lat, lon, address));
                    rvCards.setVisibility(View.VISIBLE);

                    setTitle(getString(R.string.current_location));
                    progressBar.setVisibility(View.GONE);

                    tvMsg.setVisibility(View.VISIBLE);
                })
        );
    }
}