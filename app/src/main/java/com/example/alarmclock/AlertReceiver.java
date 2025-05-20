package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

/**
 * This class receives/processes the broadcasts transmitted when an alarm (scheduled by Alarm Manager) is triggered.
 * It receives the alarm's information, plays its ringtone, and launches its accompanying notification.
 */

public class AlertReceiver extends BroadcastReceiver {
    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            int ringingAlarmId = intent.getIntExtra("EXTRA_RINGING_ALARM_ID", -1);
            Alarm alarm = AlarmApplication.getDatabaseHelper().getAlarmById(ringingAlarmId);
                if (alarm != null) {
                    wakeUpScreen(context);
                    startRingtoneService(ringingAlarmId, alarm.getRingtoneUri(), alarm.getRingtoneVol(), context);
                    openNotification(context, alarm);
                }
        }
    }

     private void wakeUpScreen(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        @SuppressWarnings("deprecation")
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK| PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "AlarmClockApp:WakeLockTag"
        );
        wakeLock.acquire(60*1000L); //keeps screen awake for 60 seconds
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (wakeLock.isHeld()) wakeLock.release();
        }, 60 * 1000L);
    }

    @SuppressLint("MissingPermission")
    private void openNotification(Context context, Alarm alarm) {
        Intent notifIntent = new Intent(context, AlarmNotificationActivity.class);
        Bundle notifBundle = new Bundle();
        notifBundle.putParcelable("BUNDLE_RINGING_ALARM", alarm);
        notifIntent.putExtras(notifBundle);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(notifIntent);
    }

    private void startRingtoneService(int alarmId, String ringtoneUri, int ringtoneVol, Context context) {
        Intent ringtoneIntent = new Intent(context, RingtoneService.class);
        ringtoneIntent.setAction("ACTION_START_RINGTONE");
        ringtoneIntent.putExtra("EXTRA_ALARM_ID", alarmId);
        ringtoneIntent.putExtra("EXTRA_RINGTONE_URI", ringtoneUri);
        ringtoneIntent.putExtra("EXTRA_RINGTONE_VOLUME", ringtoneVol);
        context.startService(ringtoneIntent);
    }
}