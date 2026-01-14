package com.jorgetp.geolocator.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jorgetp.geolocator.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

public class SavedLocationsAdapter extends RecyclerView.Adapter<CardViewHolder> {
    private final Context context;
    private JSONArray savedLocations;

    public SavedLocationsAdapter(Context context) {
        this.context = context;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
        try {
            savedLocations = new JSONArray(prefs.getString("saved_locations", "[]"));
        } catch (JSONException e) {
            //e.printStackTrace();
            savedLocations = new JSONArray();
        }
    }

    public void addItem(double lat, double lng, String address) {
        try {
            JSONObject item = new JSONObject();

            item.put("id", UUID.randomUUID().toString());
            item.put("lat", lat);
            item.put("lng", lng);
            item.put("address", address);
            item.put("time", System.currentTimeMillis());

            savedLocations.put(item);

            SharedPreferences prefs = context.getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
            prefs.edit().putString("saved_locations", savedLocations.toString()).apply();

            notifyItemInserted(0);

        } catch (JSONException e) {
            // e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return savedLocations.length();
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int p) {
        try {
            int position = getItemCount() - p - 1;

            JSONObject savedLocation = savedLocations.getJSONObject(position);

            String address = savedLocation.getString("address");
            long time = savedLocation.getLong("time");

            holder.ivIcon.setImageResource(R.drawable.outline_location_searching_24);
            holder.tvTitle.setText(new Date(time).toString());
            holder.tvValue.setText(address);

            // Add click listener for dropdown menu
            holder.itemView.setOnClickListener(v ->
                    showPopupMenu(v, holder.getBindingAdapterPosition(), savedLocation));

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void showPopupMenu(View view, int position, JSONObject savedLocation) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.saved_location_menu, popup.getMenu());

        try {
            String address = savedLocation.getString("address");
            double lat = savedLocation.getDouble("lat");
            double lng = savedLocation.getDouble("lng");
            String id = savedLocation.getString("id");

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_copy_address) {
                    copyAddressToClipboard(address);
                    return true;
                } else if (itemId == R.id.action_open_maps) {
                    openInGoogleMaps(lat, lng);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    deleteItem(position, id);
                    return true;
                }
                return false;
            });

            popup.show();

        } catch (JSONException e) {
            // e.printStackTrace();
        }
    }

    private void copyAddressToClipboard(String address) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Address", address);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void openInGoogleMaps(double lat, double lng) {
        Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        context.startActivity(intent);

        /*// Fallback to browser if Google Maps isn't installed
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            // Open in browser as fallback
            String browserUri = String.format("https://maps.google.com/?q=%f,%f", lat, lng);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri));
            context.startActivity(browserIntent);
        }*/
    }

    private void deleteItem(int position, String id) {
        try {
            // Remove from JSONArray
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < savedLocations.length(); i++) {
                if (!id.equals(savedLocations.getJSONObject(i).getString("id"))) {
                    newArray.put(savedLocations.get(i));
                }
            }
            savedLocations = newArray;

            // Save to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
            prefs.edit().putString("saved_locations", savedLocations.toString()).apply();

            // Notify adapter
            notifyItemRemoved(position);

        } catch (JSONException e) {
            // e.printStackTrace();
        }
    }
}