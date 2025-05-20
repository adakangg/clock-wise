package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This custom adapter class is used to populate a Spinner with timezone data.
 * It sets custom styling for the spinner's appearance and its dropdown items.
 */

public class SpinnerAdapterZones extends ArrayAdapter<String> {
    private final String[] offsets;
    private final String[] names;
    private int selectedPosition;
    private final Map<String, Integer> cityFlagMap = new HashMap<String, Integer>() {{
        put("America", R.drawable.usa_flag);
        put("Auckland", R.drawable.new_zealand_flag);
        put("Bangkok", R.drawable.thailand_flag);
        put("Sao Paolo", R.drawable.brazil_flag);
        put("Dubai", R.drawable.uae_flag);
        put("Kolkata", R.drawable.india_flag);
        put("Midway", R.drawable.midway_flag);
        put("Tokyo", R.drawable.japan_flag);
        put("Shanghai", R.drawable.china_flag);
        put("Azores", R.drawable.azores_flag);
        put("Australia", R.drawable.australia_flag);
        put("Athens", R.drawable.greece_flag);
        put("Berlin", R.drawable.germany_flag);
        put("London", R.drawable.england_flag);
        put("Honolulu", R.drawable.hawaii_flag);
    }};

    public SpinnerAdapterZones(Context context, String[] offsets, String[] names, int selectedIndex) {
        super(context, R.layout.spinner_item_zone, offsets);
        this.offsets = offsets;
        this.names = names;
        this.selectedPosition = selectedIndex;
    }

     public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item_zone, parent, false);
        }
        setItemStyle(convertView, position);
        return convertView;
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item_zone, parent, false);
        }
        setItemStyle(convertView, position);
        setItemBackground(convertView, position);
        return convertView;
    }

    private void setItemStyle(View convertView, int position) {
        TextView tvZoneName = convertView.findViewById(R.id.text_zone_name);
        TextView tvOffset = convertView.findViewById(R.id.text_zone_offset);
        String offset = "(UTC " + offsets[position] + ")";
        tvOffset.setText(offset);

        String fmtedZoneName = names[position].replace('_', ' ');
        String[] parts = fmtedZoneName.split("/");
        String country = parts[0];
        String city = parts[1];
        tvZoneName.setText(fmtedZoneName);

        if (Objects.equals(country, "America") || Objects.equals(country, "Australia")) city = country;
        ImageView imgFlag = convertView.findViewById(R.id.img_flag);
        Integer flagResId = cityFlagMap.get(city);
        if (flagResId != null) {
            imgFlag.setImageResource(flagResId);
        } else {
            imgFlag.setImageResource(R.drawable.usa_flag);
        }
    }


    /* used to highlight currently selected item when spinner is open */
    private void setItemBackground(View convertView, int position) {
        int bgDrawable;
        boolean isSelected = position == selectedPosition;
        if (position == 0) {
            bgDrawable = isSelected ? R.drawable.spn_dropdown_top_selected : R.drawable.spn_dropdown_top_unselected;
        } else if (position < offsets.length - 1) {
            bgDrawable = isSelected ? R.drawable.spn_dropdown_mid_selected : R.drawable.spn_dropdown_mid_unselected;
        } else {
            bgDrawable = isSelected ? R.drawable.spn_dropdown_end_selected : R.drawable.spn_dropdown_end_unselected;
        }
        convertView.setBackgroundResource(bgDrawable);
    }
}