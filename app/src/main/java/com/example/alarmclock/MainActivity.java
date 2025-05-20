package com.example.alarmclock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * This activity displays the main user interface for accessing all user-created alarms and the UTC Converter.
 * Implements `DeleteModeInterface` to synchronize layout changes with `AlarmsFragment` during `Delete Mode`
 */

public class MainActivity extends AppCompatActivity implements DeleteModeInterface {
    private Handler handler;
    private ViewPagerAdapter adapter;
    private TextView tvMainDate;
    private ZonedDateTime currentDateTime;
    private boolean inDeleteMode;

    @SuppressLint({"MissingInflatedId", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        checkOverlayPermission();
        startDateChecker();
        setDate();
        setupTabs();
    }

    /* check/ask for overlay permission when app first runs */
    private void checkOverlayPermission() {
        SharedPreferences preferences = AlarmApplication.getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        if (preferences.getBoolean("PREF_IS_FIRST_RUN", true)) {
            editor.putBoolean("PREF_IS_FIRST_RUN", false);
            editor.apply();
            showOverlayPermissionDialog();
        }
    }

    private void showOverlayPermissionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.title_dialog);
        tvDialogTitle.setText(R.string.dialog_title_overlay);
        tvDialogTitle.setTextSize(20);
        TextView tvDialogMsg = dialogView.findViewById(R.id.text_msg_dialog);
        tvDialogMsg.setText(R.string.dialog_overlay_msg);
        tvDialogMsg.setTextSize(15);
        tvDialogMsg.setVisibility(View.VISIBLE);
        LinearLayout snoozePickerLayout = dialogView.findViewById(R.id.layout_snooze_picker);
        snoozePickerLayout.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setPositiveButton("Go to Settings", (dialog, id) -> {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                })
                .setNegativeButton("Dismiss", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corners_bg);
        dialog.show();
    }

    private void setDate() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EE, d MMMM");
        DateTimeFormatter amPmFormatter = DateTimeFormatter.ofPattern("a");
        DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("zzzz");

        tvMainDate = findViewById(R.id.text_date_main);
        String fmtedDate = currentDateTime.format((dateFormatter)).toUpperCase();
        tvMainDate.setText(fmtedDate);

        TextView tvAmPm = findViewById(R.id.textclock_am_pm);
        tvAmPm.setText(currentDateTime.format(amPmFormatter));

        TextView tvZone = findViewById(R.id.text_zone);
        tvZone.setText(currentDateTime.format(zoneFormatter));
    }

    private void startDateChecker() {
        currentDateTime = ZonedDateTime.now();
        int minutesUntilNextHour = 60 - currentDateTime.getMinute();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // update date & am/pm label as necessary
                ZonedDateTime newDateTime = ZonedDateTime.now();
                if (newDateTime.toLocalDate().isAfter(currentDateTime.toLocalDate())) {
                    currentDateTime = newDateTime;
                    setDate();
                }
                handler.postDelayed(this, 60 * 60 * 1000); // check every hour
            }
        }, (long) minutesUntilNextHour * 60 * 1000); // delay first execution until next hour starts
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.viewpager_alarms);
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.alarm);
                    break;
                case 1:
                    tab.setIcon(R.drawable.timezone);
                    break;
                case 2:
                    tab.setIcon(R.drawable.theme);
                default:
                    break;
            }
        }).attach();
        inDeleteMode = false;
    }


    /* When in `DeleteMode`, date section shows number of alarms currently selected instead */
    @Override
    public void onAlarmClick(int numAlarmsSelected) {
        if (numAlarmsSelected == -1) {
            setDate();
            inDeleteMode = false;
        } else {
            String alarmsSelectedStr = numAlarmsSelected + " selected";
            tvMainDate.setText(alarmsSelectedStr);
            inDeleteMode = true;
        }
    }

    /**
     * @noinspection deprecation
     */
    @Override
    public void onBackPressed() {
        if (inDeleteMode) {
            AlarmsFragment fragment = (AlarmsFragment) adapter.getFragment(0);
            if (fragment != null) fragment.onBackPress();
            inDeleteMode = false;
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}