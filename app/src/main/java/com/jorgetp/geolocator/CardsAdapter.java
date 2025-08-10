package com.jorgetp.geolocator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> {
    private final Context context;
    private final double lat;
    private final double lng;
    private final String address;
    private final String plusCode;

    public CardsAdapter(Context context, double lat, double lng, String address, String plusCode) {
        this.context = context;
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.plusCode = plusCode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        switch (position) {
            case 0:
                holder.ivIcon.setImageResource(R.drawable.outline_location_searching_24);
                holder.tvTitle.setText("Coordinates");
                holder.tvValue.setText(lat + ", " + lng);
                holder.card.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Location Coordinates", lat + ", " + lng);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Coordinates copied", Toast.LENGTH_SHORT).show();
                });
                break;
            case 1:
                holder.ivIcon.setImageResource(R.drawable.outline_home_24);
                holder.tvTitle.setText("Address");
                holder.tvValue.setText(address);
                holder.card.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Location Address", address);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show();
                });
                break;
            case 2:
                holder.ivIcon.setImageResource(R.drawable.outline_map_24);
                holder.tvTitle.setText("Google Maps Plus Code");
                holder.tvValue.setText(plusCode);
                holder.card.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Location Plus Code", plusCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Plus Code copied", Toast.LENGTH_SHORT).show();
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


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        CardView card;
        TextView tvTitle, tvValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            card = itemView.findViewById(R.id.card);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvValue = itemView.findViewById(R.id.tv_value);
        }
    }
}
