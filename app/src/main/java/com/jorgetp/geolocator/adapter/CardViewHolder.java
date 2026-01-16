package com.jorgetp.geolocator.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.jorgetp.geolocator.R;

public class CardViewHolder extends RecyclerView.ViewHolder {
    public ImageView ivIcon;
    public TextView tvTitle;
    public TextView tvValue;
    public ChipGroup chipGroup;

    public CardViewHolder(@NonNull View itemView) {
        super(itemView);
        ivIcon = itemView.findViewById(R.id.iv_icon);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvValue = itemView.findViewById(R.id.tv_value);
        chipGroup = itemView.findViewById(R.id.chip_group);
    }
}