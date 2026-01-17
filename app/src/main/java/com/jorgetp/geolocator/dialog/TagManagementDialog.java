package com.jorgetp.geolocator.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.jorgetp.geolocator.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TagManagementDialog {
    private final Context context;
    private final JSONObject location;
    private final OnTagsUpdatedListener listener;

    public TagManagementDialog(Context context, JSONObject location, OnTagsUpdatedListener listener) {
        this.context = context;
        this.location = location;
        this.listener = listener;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tag_management, null);

        ChipGroup chipGroup = dialogView.findViewById(R.id.chip_group_tags);
        EditText editNewTag = dialogView.findViewById(R.id.edit_new_tag);

        // Load existing tags
        loadExistingTags(chipGroup);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.manage_tags)
                .setView(dialogView)
                .setNegativeButton(R.string.add_tag, null)
                .setPositiveButton(R.string.done, (d, which) -> {
                    updateLocationTags(chipGroup);
                    d.dismiss();
                })
                .create();

        dialog.show();

        // Override the positive button to prevent dialog dismissal
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            String newTag = editNewTag.getText().toString().trim().toLowerCase();
            if (!newTag.isEmpty()) {
                addTagChip(chipGroup, newTag);
                editNewTag.setText("");
            } else {
                Toast.makeText(context, R.string.please_enter_a_tag, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExistingTags(ChipGroup chipGroup) {
        try {
            JSONArray tagsArray = location.optJSONArray("tags");
            if (tagsArray != null) {
                for (int i = 0; i < tagsArray.length(); i++) {
                    String tag = tagsArray.getString(i);
                    addTagChip(chipGroup, tag);
                }
            }
        } catch (JSONException e) {
            // Handle error
        }
    }

    private void addTagChip(ChipGroup chipGroup, String tagText) {
        // Check if tag already exists
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip existingChip = (Chip) chipGroup.getChildAt(i);
            if (existingChip.getText().toString().equalsIgnoreCase(tagText)) {
                Toast.makeText(context, R.string.tag_already_exists, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Chip chip = new Chip(context);
        chip.setText(tagText);
        chip.setCloseIconVisible(true);
        //chip.setChipStrokeColorResource(android.R.color.transparent);
        chip.setOnCloseIconClickListener(v -> chipGroup.removeView(chip));

        chipGroup.addView(chip);
    }

    private void updateLocationTags(ChipGroup chipGroup) {
        try {
            JSONArray tagsArray = new JSONArray();
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
                tagsArray.put(chip.getText().toString());
            }

            location.put("tags", tagsArray);
            if (listener != null) {
                listener.onTagsUpdated(location);
            }
        } catch (JSONException e) {
            Toast.makeText(context, "Error saving tags", Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnTagsUpdatedListener {
        void onTagsUpdated(JSONObject updatedLocation);
    }
}