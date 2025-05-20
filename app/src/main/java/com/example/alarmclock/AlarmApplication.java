package com.example.alarmclock;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import org.maplibre.android.MapLibre;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class initializes/maintains global app state & acts as a centralized place to store/access shared resources.
 */

public class AlarmApplication extends Application {
    private static AlarmApplication instance;
    private static DatabaseHelper databaseHelper;
    private static SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        instance.setTheme(R.style.Theme_AlarmClock);
        databaseHelper = new DatabaseHelper(this);
        NotificationHelper.createNotificationChannel(instance.getApplicationContext());
        MapLibre.getInstance(instance);
        cacheTimeZones();
    }


    /* create/store map of timezone names to their offsets in SharedPreferences */
    private void cacheTimeZones() {
        Map<String, String> zoneNameOffsets = new TreeMap<>();
        String[] zones = getResources().getStringArray(R.array.zones);
        DateTimeFormatter offsetFormatter = DateTimeFormatter.ofPattern("xxx");

        // store the first found offset for each zone
        for (String zoneId : zones) {
            ZoneId zone = ZoneId.of(zoneId);
            ZoneOffset offset = zone.getRules().getOffset(Instant.now());
            zoneNameOffsets.put(offsetFormatter.format(offset), zoneId);
        }

        // order alphabetically by zone name
        List<Map.Entry<String, String>> entryList = new ArrayList<>(zoneNameOffsets.entrySet());
        entryList.sort(Map.Entry.comparingByValue());
        Map<String, String> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        SharedPreferences.Editor editor = preferences.edit();
        String fmtedMap = new Gson().toJson(sortedMap);
        editor.putString("PREF_ZONES_MAP", fmtedMap);
        editor.apply();
    }

    public static AlarmApplication getInstance() {
        return instance;
    }

    public static DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    public static SharedPreferences getSharedPreferences() {
        return preferences;
    }
}