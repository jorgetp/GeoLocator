package com.jorgetp.geolocator.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jorgetp.geolocator.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SavedLocationsAdapter extends RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Pair<Long, JSONObject>> savedLocations = new ArrayList<>();

    public SavedLocationsAdapter(Context context) {
        this.context = context;

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("saved_locations", Context.MODE_PRIVATE);
        for (int i = 0; i < prefs.getAll().size(); i++) {
            String key = prefs.getAll().keySet().toArray()[i].toString();
            String value = prefs.getString(key, "{}");
            try {
                JSONObject jsonObject = new JSONObject(value);
                savedLocations.add(new Pair<>(Long.parseLong(key), jsonObject));
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }
        savedLocations.sort((o1, o2) -> o2.first.compareTo(o1.first));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.saved_location_item, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy, h:mm a", Locale.getDefault());
        String formattedTime = sdf.format(new Date(savedLocations.get(position).first));
        formattedTime = formattedTime.substring(0, 1).toUpperCase() + formattedTime.substring(1);

        holder.tvTime.setText(formattedTime);

        String lat = savedLocations.get(position).second.optString("lat");
        String lng = savedLocations.get(position).second.optString("lng");
        String address = savedLocations.get(position).second.optString("address");
        String plusCode = savedLocations.get(position).second.optString("plus_code");

        holder.tvAddress.setText(address);
        holder.card.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, v);
            popup.getMenuInflater().inflate(R.menu.menu_saved_location_popup, popup.getMenu());
            MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.copy_coordinates) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Coordinates", String.format("%s, %s", lat, lng));
                    clipboard.setPrimaryClip(clip);
                    // Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();

                } else if (item.getItemId() == R.id.copy_address) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Address", address);
                    clipboard.setPrimaryClip(clip);
                    // Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();

                } else if (item.getItemId() == R.id.copy_plus_code) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Plus Code", plusCode);
                    clipboard.setPrimaryClip(clip);
                    // Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();

                } else if (item.getItemId() == R.id.delete) {
                    new AlertDialog.Builder(context)
                            .setMessage(R.string.delete_confirmation)
                            .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                                int p = holder.getAdapterPosition();

                                context.getApplicationContext()
                                        .getSharedPreferences("saved_locations", Context.MODE_PRIVATE)
                                        .edit()
                                        .remove("" + savedLocations.get(p).first)
                                        .apply();

                                savedLocations.remove(p);
                                notifyItemRemoved(p);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show();
                    return true;

                }
                return true;
            });

            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return savedLocations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View card;
        TextView tvTime, tvAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAddress = itemView.findViewById(R.id.tv_address);
        }
    }
}
