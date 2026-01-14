package com.jorgetp.geolocator.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jorgetp.geolocator.R;

public class CurrentLocationAdapter extends RecyclerView.Adapter<CardViewHolder> {
    private final Context context;
    private final double lat;
    private final double lng;
    private final String address;

    public CurrentLocationAdapter(Context context, double lat, double lng, String address) {
        this.context = context;
        this.lat = lat;
        this.lng = lng;
        this.address = address;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        switch (position) {
            case 0:
                holder.ivIcon.setImageResource(R.drawable.outline_location_searching_24);
                holder.tvTitle.setText(R.string.coordinates);
                holder.tvValue.setText(String.format("%s, %s", lat, lng));
                holder.card.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Coordinates", lat + ", " + lng);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                });
                break;
            case 1:
                holder.ivIcon.setImageResource(R.drawable.outline_home_24);
                holder.tvTitle.setText(R.string.address);
                holder.tvValue.setText(address);
                holder.card.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Address", address);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                });
                break;
            case 2:
                Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                holder.ivIcon.setImageResource(R.drawable.outline_map_24);
                holder.tvTitle.setText(R.string.google_maps);
                holder.tvValue.setText(uri.toString());
                holder.card.setOnClickListener(v -> {
                    /*ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Plus Code", plusCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();*/

                    // Open Google Maps with the provided coordinates
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage("com.google.android.apps.maps");
                    context.startActivity(intent);
                });
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
