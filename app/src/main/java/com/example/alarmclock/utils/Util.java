package com.example.alarmclock.utils;

import android.content.SharedPreferences;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import com.example.alarmclock.AlarmApplication;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a utility class that defines functions commonly used throughout the app.
 */
public class Util {
    public static boolean isValidInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int get24HrFrom12Hr(int hr12, String amPm) {
        boolean isAm = String.valueOf(amPm).equalsIgnoreCase("AM");
        return isAm ? (hr12 % 12) : (hr12 % 12) + 12;
    }

    public static String formatTimeAmPm(ZonedDateTime zdt) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        return zdt.format(timeFormatter);
    }

    public static String formatZdtDate(ZonedDateTime zdt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, y");
        return zdt.format(formatter);
    }

    public static String formatZdtDateDayOfWeek(ZonedDateTime zdt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, MMMM d");
        return zdt.format(formatter);
    }


    /* returns index/position of specified offset from stored zone map */
    public static int getZoneIndex( Map<String, String> zonesMap, ZoneOffset targetOffset) {
        String offsetStr = String.valueOf(targetOffset);
        List<Map.Entry<String, String>> zones = new ArrayList<>(zonesMap.entrySet());
        for (int i = 0; i < zones.size(); i++) {
            String zoneOffset = zones.get(i).getKey();
            if (zoneOffset.equals(offsetStr)) return i;
        }
        return -1;
    }


    /* returns name matching specified zone offset from stored zone map */
    public static String getZoneOffsetName(ZoneOffset targetOffset) {
        Map<String, String> zonesMap = Util.getTimeZoneMap();
        String offsetStr = String.valueOf(targetOffset);
        List<Map.Entry<String, String>> zones = new ArrayList<>(zonesMap.entrySet());
        for (int i = 0; i < zones.size(); i++) {
            String zoneOffset = zones.get(i).getKey();
            if (zoneOffset.equals(offsetStr)) return zones.get(i).getValue();
        }
        return "";
    }


    /* returns map of timezone names and offsets stored in SharedPreferences */
    public static Map<String, String> getTimeZoneMap() {
        SharedPreferences prefs = AlarmApplication.getSharedPreferences();
        String json = prefs.getString("PREF_ZONES_MAP", null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    public static String formatOffset(ZoneOffset offset) {
        String fmtedOffset = String.valueOf(offset);
        if (fmtedOffset.equals("Z") || fmtedOffset.equals("+00:00")) return "UTC Z";

        // remove leading zeroes in hr and trailing zeroes in min
        fmtedOffset = fmtedOffset.replaceFirst("([+-])0?(\\d+):(\\d+)", "$1$2:$3");
        if (fmtedOffset.endsWith(":00")) {
            fmtedOffset = fmtedOffset.substring(0, fmtedOffset.length() - 3);
        }
        return "UTC " + fmtedOffset;
    }

    public static void hideKeyboard(EditText input) {
        InputMethodManager imm = ContextCompat.getSystemService(AlarmApplication.getInstance(), InputMethodManager.class);
        if (imm != null && input.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }
}