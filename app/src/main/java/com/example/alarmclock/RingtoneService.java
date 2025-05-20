package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;

/**
 * This service manages audio playback of ringtones when alarms are triggered.
 */

public class RingtoneService extends Service {
    private AudioManager audioManager;
    private Ringtone ringtone;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals("ACTION_START_RINGTONE")) {
                stopRingtone();
                setRingingAlarmPref(intent);

                int ringtoneVol = intent.getIntExtra("EXTRA_RINGTONE_VOLUME", 0);
                Uri ringtoneUri = Uri.parse(intent.getStringExtra("EXTRA_RINGTONE_URI"));
                audioManager.setStreamVolume(AudioManager.STREAM_RING, ringtoneVol, 0);
                ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                ringtone.play();
            } else if (action.equals("ACTION_STOP_RINGTONE")) {
                stopRingtone();
            }
        }
        return START_NOT_STICKY;
    }


    /* set currently ringing alarm flag in preferences
       if previously triggered alarm was not handled, replace it here so app only tracks most recent triggered alarm
     */
    @SuppressLint("ApplySharedPref")
    private void setRingingAlarmPref(Intent intent) {
        int alarmId = intent.getIntExtra("EXTRA_ALARM_ID", -1);
        SharedPreferences.Editor editor = AlarmApplication.getSharedPreferences().edit();
        editor.putInt("PREF_RINGING_ALARM_ID", alarmId);
        editor.commit();
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}