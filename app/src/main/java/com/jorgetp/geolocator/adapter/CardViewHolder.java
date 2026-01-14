package com.jorgetp.geolocator.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jorgetp.geolocator.R;

public class CardViewHolder extends RecyclerView.ViewHolder {
    ImageView ivIcon;
    View card;
    TextView tvTitle, tvValue;

    public CardViewHolder(@NonNull View itemView) {
        super(itemView);
        ivIcon = itemView.findViewById(R.id.iv_icon);
        card = itemView.findViewById(R.id.card);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvValue = itemView.findViewById(R.id.tv_value);
    }
}