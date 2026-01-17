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

import com.google.android.material.chip.Chip;
import com.jorgetp.geolocator.R;
import com.jorgetp.geolocator.dialog.TagManagementDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ItemsAdapter extends RecyclerView.Adapter<CardViewHolder> {
    private final Context context;
    private final double lat;
    private final double lng;
    private final String address;

    private JSONArray savedLocations;

    public ItemsAdapter(Context context, double lat, double lng, String address) {
        this.context = context;
        this.lat = lat;
        this.lng = lng;
        this.address = address;

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
        try {
            savedLocations = new JSONArray(prefs.getString("saved_locations", "[]"));
        } catch (JSONException e) {
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
            item.put("tags", new JSONArray()); // Initialize with empty tags

            savedLocations.put(item);

            SharedPreferences prefs = context.getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
            prefs.edit().putString("saved_locations", savedLocations.toString()).apply();

            notifyItemInserted(4);

        } catch (JSONException e) {
            // e.printStackTrace();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 3)
            return R.layout.current_location_item;
        else if (position == 3)
            return R.layout.header;
        else
            return R.layout.saved_location_item;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return savedLocations.length() + 4;
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int p) {
        switch (p) {
            case 0:
                holder.ivIcon.setImageResource(R.drawable.outline_location_searching_24);
                holder.tvTitle.setText(R.string.coordinates);
                holder.tvValue.setText(String.format("%s, %s", lat, lng));
                holder.itemView.setOnClickListener(v -> {
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
                holder.itemView.setOnClickListener(v -> {
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
                holder.tvValue.setText(R.string.click_to_open);
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage("com.google.android.apps.maps");
                    context.startActivity(intent);
                });
                break;
            case 3:
                break;
            default:
                try {
                    int position = getItemCount() - p - 1;
                    JSONObject savedLocation = savedLocations.getJSONObject(position);

                    holder.tvTitle.setText(formatTime(savedLocation.getLong("time")));
                    holder.tvValue.setText(savedLocation.getString("address"));

                    // Display tags
                    displayTags(holder, savedLocation);

                    // Add click listener for dropdown menu
                    holder.itemView.setOnClickListener(v ->
                            showPopupMenu(v, holder.getBindingAdapterPosition(), savedLocation));

                } catch (JSONException e) {
                    // e.printStackTrace();
                }
                break;
        }
    }

    private void displayTags(CardViewHolder holder, JSONObject savedLocation) {
        try {
            JSONArray tagsArray = savedLocation.optJSONArray("tags");
            if (tagsArray != null && tagsArray.length() > 0) {
                holder.chipGroup.setVisibility(View.VISIBLE);
                holder.chipGroup.removeAllViews();

                for (int i = 0; i < tagsArray.length(); i++) {
                    String tag = tagsArray.getString(i);
                    Chip chip = new Chip(context);
                    chip.setText(tag);
                    //chip.setChipStrokeColorResource(android.R.color.transparent);
                    chip.setChipStrokeColorResource(R.color.tag_border);
                    holder.chipGroup.addView(chip);
                }
            } else {
                holder.chipGroup.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            holder.chipGroup.setVisibility(View.GONE);
        }
    }

    private String formatTime(long timestamp) {
        Date date = new Date(timestamp);
        /*long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60 * 1000) {
            return "Just now";
        }

        if (diff < 60 * 60 * 1000) {
            int minutes = (int) (diff / (60 * 1000));
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        }

        if (diff < 24 * 60 * 60 * 1000) {
            int hours = (int) (diff / (60 * 60 * 1000));
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        }

        if (diff < 7 * 24 * 60 * 60 * 1000) {
            int days = (int) (diff / (24 * 60 * 60 * 1000));
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }*/


        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy, h:mm a", Locale.getDefault());
        // capitalize first letter
        char[] chars = sdf.format(date).toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
        //return sdf.format(date);
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
                } else if (itemId == R.id.action_manage_tags) {
                    showTagManagementDialog(position, savedLocation);
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

    private void showTagManagementDialog(int position, JSONObject savedLocation) {
        TagManagementDialog dialog = new TagManagementDialog(context, savedLocation, (updatedLocation) -> {
            try {
                // Update the location in the array
                int actualPosition = getItemCount() - position - 1;
                savedLocations.put(actualPosition, updatedLocation);

                // Save to SharedPreferences
                SharedPreferences prefs = context.getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
                prefs.edit().putString("saved_locations", savedLocations.toString()).apply();

                // Refresh the item
                notifyItemChanged(position);
            } catch (JSONException e) {
                Toast.makeText(context, "Error updating tags", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
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
    }

    private void deleteItem(int position, String id) {
        try {
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < savedLocations.length(); i++) {
                if (!id.equals(savedLocations.getJSONObject(i).getString("id"))) {
                    newArray.put(savedLocations.get(i));
                }
            }
            savedLocations = newArray;

            SharedPreferences prefs = context.getSharedPreferences("geo_locator", Context.MODE_PRIVATE);
            prefs.edit().putString("saved_locations", savedLocations.toString()).apply();

            notifyItemRemoved(position);

        } catch (JSONException e) {
            // e.printStackTrace();
        }
    }
}