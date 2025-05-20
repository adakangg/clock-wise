package com.example.alarmclock;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alarmclock.utils.Util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * This activity is used to notify the user that an alarm has been triggered.
 * It provides details about the ringing alarm and options for users to snooze or dismiss the alarm.
 */

public class AlarmNotificationActivity extends AppCompatActivity {
    private Context context;
    private View overlayView;
    private Alarm alarm;
    private Handler handler;

    @SuppressLint({"MissingInflatedId", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_notification);
        overlayView = LayoutInflater.from(this).inflate(R.layout.activity_alarm_notification, null);
        context = AlarmApplication.getInstance().getApplicationContext();
        handler = new Handler(Looper.getMainLooper());

        Bundle receivedBundle = getIntent().getExtras();
        handleBundle(receivedBundle);

        this.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        Button btnDismiss = findViewById(R.id.btn_dismiss);
        btnDismiss.setOnClickListener(v -> dismissAlarm());
        Button btnSnooze = findViewById(R.id.btn_snooze);
        btnSnooze.setOnClickListener(v -> snoozeAlarm());
    }

    /* resets alarm data if second alarm is triggered while previous alarm had not been dismissed */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Bundle secondBundle = getIntent().getExtras();
        handleBundle(secondBundle);
    }

    private void handleBundle(Bundle bundle) {
        if (bundle != null) {
            Alarm bundleAlarm = bundle.getParcelable("BUNDLE_RINGING_ALARM");
            if (bundleAlarm != null) {
                alarm = bundleAlarm;
                setAlarmData();
            }
        }
    }

    private void setAlarmData() {
        ZonedDateTime zdtAlarm = alarm.getZdt();
        TextView tvAlarmName = findViewById(R.id.text_alarm_name_notif);
        tvAlarmName.setText(alarm.getName());

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
        TextView tvAlarmTime = findViewById(R.id.text_alarm_time_notif);
        tvAlarmTime.setText(zdtAlarm.format(timeFormatter));

        DateTimeFormatter amPmFormatter = DateTimeFormatter.ofPattern("a");
        TextView tvAlarmAmPM = findViewById(R.id.text_alarm_ampm_notif);
        tvAlarmAmPM.setText(zdtAlarm.format(amPmFormatter));

        TextView tvAlarmZone = findViewById(R.id.text_alarm_zone_notif);
        ZoneId zone = ZoneId.of(Util.getZoneOffsetName(alarm.getZdt().getOffset()));
        String fullZoneName = zone.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        tvAlarmZone.setText(fullZoneName);

        TextView tvAlarmDate = findViewById(R.id.text_alarm_date_notif);
        if (alarm.isWeeklyAlarm()) {
            String weeklySchedStr = alarm.weeklySchedToAbbrev(alarm.getWeeklySched(), "  ");
            tvAlarmDate.setText(weeklySchedStr);
        } else {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EE, MMMM d");
            tvAlarmDate.setText(zdtAlarm.format(dateFormatter).toUpperCase());
        }
    }

    private void stopRingtoneService() {
        Intent ringtoneIntent = new Intent(context, RingtoneService.class);
        ringtoneIntent.setAction("ACTION_STOP_RINGTONE");
        context.startService(ringtoneIntent);
    }


    /* resets alarm to trigger after designated snooze time */
    @SuppressLint("ScheduleExactAlarm")
    private void snoozeAlarm() {
        int snoozeLen = alarm.getSnoozeLenMins();
        if (snoozeLen > 0) {
            Instant snoozeInstant = Instant.ofEpochMilli(System.currentTimeMillis() + (snoozeLen * 60000L));
            ZonedDateTime snoozeZdt = ZonedDateTime.ofInstant(snoozeInstant, ZoneId.systemDefault());
            alarm.setZdt(snoozeZdt);
            alarm.setIsSnoozed(true);
            alarm.setAlarm(context);
            Toast.makeText(this, "Snoozed for " + snoozeLen + " minute(s)", Toast.LENGTH_SHORT).show();
            closeActivity();
        } else {
            Toast.makeText(this, "Snooze disabled", Toast.LENGTH_SHORT).show();
        }
    }


    /* reschedules valid weekly alarms & cancel/deactivates 1-time alarms/duplicate weekly alarms */
    private void dismissAlarm() {
        if (alarm.isWeeklyAlarm()) {
            ZonedDateTime nextZdt = Alarm.calcWeeklyAlarmDate(alarm.getZdt(), alarm.getWeeklySched());
            boolean hasDuplicate = Alarm.hasDuplicateAlarm(alarm.getId(), nextZdt, alarm.getWeeklySched());
            if (!hasDuplicate) {
                // calculate/reset weekly alarm
                alarm.setZdt(nextZdt);
                AlarmApplication.getDatabaseHelper().addAlarm(alarm);
                alarm.setAlarm(context);
            } else {
                Toast.makeText(this, alarm.getName() + " deactivated due to duplicate", Toast.LENGTH_SHORT).show();
            }
        }
        if (alarm.getIsSnoozed()) alarm.setIsSnoozed(false);
        closeActivity();
    }

    @SuppressLint("ApplySharedPref")
    private void closeActivity() {
        SharedPreferences preferences = AlarmApplication.getSharedPreferences();
        int prefAlarmId = preferences.getInt("PREF_RINGING_ALARM_ID", -1) ;
        if (prefAlarmId == alarm.getId()) { // remove alarm from preferences to mark as handled
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("PREF_RINGING_ALARM_ID");
            editor.commit();
        }
        stopRingtoneService();
        handler.postDelayed(this::dismissOverlayActivity, 1000);
    }

    private void dismissOverlayActivity() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (overlayView.getParent() != null) windowManager.removeView(overlayView);
        handler.removeCallbacksAndMessages(null);
        finish();
    }
}