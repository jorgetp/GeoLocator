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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jorgetp.geolocator.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SavedLocationsAdapter extends RecyclerView.Adapter<SavedLocationsAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Pair<Long, String>> savedLocations = new ArrayList<>();

    public SavedLocationsAdapter(Context context) {
        this.context = context;

        SharedPreferences sharedPreferences = context.getSharedPreferences("saved_locations", Context.MODE_PRIVATE);
        for (int i = 0; i < sharedPreferences.getAll().size(); i++) {
            String key = sharedPreferences.getAll().keySet().toArray()[i].toString();
            String value = sharedPreferences.getString(key, "");
            savedLocations.add(new Pair<>(Long.parseLong(key), value));
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
        holder.tvAddress.setText(savedLocations.get(position).second);
        holder.card.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, v);
            popup.getMenuInflater().inflate(R.menu.saved_location_popup_menu, popup.getMenu());
            MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.copy) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Location Address", savedLocations.get(position).second);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, R.string.address_copied, Toast.LENGTH_SHORT).show();

                } else if (item.getItemId() == R.id.delete) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences("saved_locations", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("" + savedLocations.get(position).first);
                    editor.apply();
                    savedLocations.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, R.string.location_deleted, Toast.LENGTH_SHORT).show();
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
