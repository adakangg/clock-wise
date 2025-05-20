package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

/**
 * This fragment provides an interface for users to toggle the app's theme.
 */

public class ThemeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_theme, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LottieAnimationView anim = view.findViewById(R.id.anim_theme);
        SwitchCompat swTheme = view.findViewById(R.id.switch_theme);
        int currentTheme = AppCompatDelegate.getDefaultNightMode();
        boolean isDarkTheme = currentTheme == AppCompatDelegate.MODE_NIGHT_YES;
        swTheme.setChecked(isDarkTheme);
        anim.setAnimation(isDarkTheme ? R.raw.moon_anim : R.raw.sun_anim);
        swTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                anim.setAnimation(R.raw.moon_anim);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                anim.setAnimation(R.raw.sun_anim);
            }
        });

        LinearLayout layoutAttributions = view.findViewById(R.id.layout_attributions);
        layoutAttributions.setOnClickListener(v -> {
            Intent attributionsIntent = new Intent(requireActivity(), AttributionsActivity.class);
            startActivity(attributionsIntent);
        });
    }
}