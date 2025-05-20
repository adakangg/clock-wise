package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.alarmclock.utils.Util;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to represent an Alarm object + store/retrieve its fields.
 * Implements Parcelable to enable data/instance transfer between Activities/Services
 * Implements Serializable to enable object serialization
 */

public class Alarm implements Parcelable, Serializable {
    private long id;
    private String name, ringtoneUri;
    private boolean isActive, isVibrOn, isSnoozed;
    private int snoozeLenMins, ringtoneVol;
    private HashSet<Integer> weeklySched; // contains integer values of days of week that alarm is scheduled to repeat on
    private ZonedDateTime zdt;

    public Alarm(String name, long id, boolean isActive, ZonedDateTime zdt, HashSet<Integer> weeklySched,
                 String ringtoneUri, int ringtoneVol, boolean isVibrOn, boolean isSnoozed, int snoozeLenMins) {
        this.name = name;
        this.id = id;
        this.isActive = isActive;
        this.zdt = zdt;
        this.weeklySched = weeklySched;
        this.ringtoneUri = ringtoneUri;
        this.ringtoneVol = ringtoneVol;
        this.isVibrOn = isVibrOn;
        this.isSnoozed = isSnoozed;
        this.snoozeLenMins = snoozeLenMins;
    }

    protected Alarm(Parcel in) {
        name = in.readString();
        id = in.readLong();
        isActive = in.readInt() == 1;
        zdt = ZonedDateTime.parse(in.readString());
        ArrayList<Integer> weeklyDayList = new ArrayList<>();
        in.readList(weeklyDayList, getClass().getClassLoader());
        weeklySched = new HashSet<>(weeklyDayList);
        ringtoneUri = in.readString();
        ringtoneVol = in.readInt();
        isVibrOn = in.readInt() == 1;
        isSnoozed = in.readInt() == 1;
        snoozeLenMins = in.readInt();
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeLong(id);
        dest.writeInt(isActive ? 1 : 0);
        dest.writeString(zdt.toString());
        ArrayList<Integer> weeklyDayList = new ArrayList<>(weeklySched);
        dest.writeList(weeklyDayList);
        dest.writeString(ringtoneUri);
        dest.writeInt(ringtoneVol);
        dest.writeInt(isVibrOn ? 1 : 0);
        dest.writeInt(isSnoozed ? 1 : 0);
        dest.writeInt(snoozeLenMins);
    }

    public static final Creator<Alarm> CREATOR = new Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }
        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };


    /* calculates the nearest date that matches the weekly alarm's schedule */
    public static ZonedDateTime calcWeeklyAlarmDate(ZonedDateTime zdtAlarm, HashSet<Integer> alarmWeeklySched) {
        String zoneName = Util.getZoneOffsetName(zdtAlarm.getOffset());
        ZonedDateTime zoneZdtNow = ZonedDateTime.now(ZoneId.of(zoneName));
        ZonedDateTime alarmZdt = zdtAlarm.withYear(zoneZdtNow.getYear())
                .withMonth(zoneZdtNow.getMonthValue())
                .withDayOfMonth(zoneZdtNow.getDayOfMonth());
        ZonedDateTime zdtNow = ZonedDateTime.now().withSecond(0).withNano(0);

        if (!alarmZdt.isAfter(zdtNow)){
            alarmZdt = alarmZdt.plusDays(1);
            zoneZdtNow = zoneZdtNow.plusDays(1);
        }

        int alarmDayOfWeek =  zoneZdtNow.getDayOfWeek().getValue();
        int min = Integer.MAX_VALUE;

        /* calculate nearest day of week */
        for (int day : alarmWeeklySched) {
            int daysBetween = day - alarmDayOfWeek;
            if (daysBetween < 0) daysBetween += 7;
            if (daysBetween < min) min = daysBetween;
        }
        return alarmZdt.plusDays(min);
    }


    /* returns true if alarm's fields are valid, otherwise returns false */
    public static boolean verifyAlarm(Context context, int alarmId, ZonedDateTime alarmZdt, HashSet<Integer> selectedDays) {
        if (!alarmZdt.isAfter(ZonedDateTime.now())) {
            if (context != null) {
                Toast.makeText(context, "Alarm date/time has passed", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        if (hasDuplicateAlarm(alarmId, alarmZdt, selectedDays)) {
            if (context != null) {
                Toast.makeText(context, "Duplicate alarm exists", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }


    /* returns true if the alarm's ZonedDateTime is identical to a currently active alarm's */
    public static boolean hasDuplicateAlarm(long targetAlarmId, ZonedDateTime targetZdt, HashSet<Integer> targetWeeklySched) {
        List<Alarm> alarms = AlarmApplication.getDatabaseHelper().getAllAlarms();
        for (Alarm alarm : alarms) {
            if (alarm.getId() != targetAlarmId && alarm.getIsActive()) {
                if (alarm.getZdt().isEqual(targetZdt)) {
                    boolean targetIsWeekly = targetWeeklySched.size() > 0;
                    if (targetIsWeekly) {
                        for (int targetDay : targetWeeklySched) {
                            return (alarm.getWeeklySched().contains(targetDay));
                        }
                    } else { // 1-time alarm
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private PendingIntent buildPendingIntent(Context context) {
        Intent notifIntent = new Intent(context, AlertReceiver.class);
        notifIntent.putExtra("EXTRA_RINGING_ALARM_ID", (int) this.getId());
        return PendingIntent.getBroadcast(context, (int) this.getId(), notifIntent, PendingIntent.FLAG_IMMUTABLE);
    }


    /* schedules `AlarmManager` to broadcast intent/trigger notification event when alarm goes off */
    @SuppressLint("ScheduleExactAlarm")
    public void setAlarm(Context context) {
        PendingIntent alarmIntent = buildPendingIntent(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, this.getZdt().toInstant().toEpochMilli(), alarmIntent);
        if (!this.getIsActive()) this.setIsActive(true);
    }


    /* cancels previously set alarm's notification intent and inactivate its status */
    public void cancelAlarm(Context context) {
        PendingIntent alarmIntent = buildPendingIntent(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);
        if (this.getIsActive()) this.setIsActive(false);
        if (this.getIsSnoozed()) this.setIsSnoozed(false);
    }

    public String getRingtoneUri() { return ringtoneUri; }

    public int getSnoozeLenMins() { return snoozeLenMins; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public int getRingtoneVol() { return ringtoneVol; }

    public boolean getIsVibrOn() { return isVibrOn; }

    public boolean getIsActive() { return isActive; }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
        AlarmApplication.getDatabaseHelper().setActiveStatus(this);
    }

    public boolean getIsSnoozed() { return isSnoozed; }

    public void setIsSnoozed(boolean isSnoozed) {
        this.isSnoozed = isSnoozed;
        AlarmApplication.getDatabaseHelper().setSnoozeStatus(this);
    }

    public void setZdt(ZonedDateTime zdt) { this.zdt = zdt; }

    public ZonedDateTime getZdt() { return zdt; }

    public HashSet<Integer> getWeeklySched() { return weeklySched; }

    public boolean isWeeklyAlarm() { return this.getWeeklySched().size() > 0; }


    /* return weekly alarm's schedule (integer values of selected days) as comma-separated string */
    public String weeklySchedToString() {
        String weeklySched = this.weeklySched.toString();
        return weeklySched.substring(1, weeklySched.length()-1).replaceAll("\\s", "");
    }


    /* return weekly schedule string as list */
    public static List<Integer> weeklySchedStrToList(String weeklySchedStr) {
        String[] strArray = weeklySchedStr.split(",");
        List<Integer> weeklySched = new ArrayList<>();
        for (String i : strArray) {
            weeklySched.add(Integer.valueOf(i));
        }
        return weeklySched;
    }


    /* return weekly alarm's schedule (abbreviations of selected days) as string */
    public String weeklySchedToAbbrev(HashSet<Integer> weeklySched, String separator) {
        Resources resources = AlarmApplication.getInstance().getResources();
        if (weeklySched.size() == 1) {
            int dayIndex = weeklySched.iterator().next();
            String[] daysOfWeek = resources.getStringArray(R.array.days_of_week);
            return daysOfWeek[dayIndex];
        } else {
            StringBuilder formattedSched = new StringBuilder();
            String[] daysOfWeekAbbrv = resources.getStringArray(R.array.days_of_week_abbrv);
            for (int day : weeklySched) {
                formattedSched.append(daysOfWeekAbbrv[day]).append(separator);
            }
            return formattedSched.toString();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Alarm{" +
                " name= " + name +
                ", alarmId = " + id +
                ", active = " + isActive +
                ", zdt = " + zdt.toString() +
                ", weeklySched = " + weeklySchedToString() +
                ", ringtoneURI = " + ringtoneUri +
                ", ringtoneVol = " + ringtoneVol +
                ", vibrateOn = " + isVibrOn +
                ", isSnoozed = " + isSnoozed +
                ", snoozeLength = " + snoozeLenMins +
                "}";
    }
}
