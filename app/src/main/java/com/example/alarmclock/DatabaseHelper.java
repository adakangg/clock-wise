package com.example.alarmclock;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;

import com.example.alarmclock.utils.Util;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * This class creates/manages the local SQLite database used to store/access alarms for this application.
 */

 public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "ALARM_DATABASE";
    private static final String TABLE_NAME = "ALARM_TABLE";
    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_IS_ACTIVE = "IS_ACTIVE";
    private static final String COLUMN_NAME = "NAME";
    private static final String COLUMN_ZONEDDATETIME = "ZONEDDATETIME";
    private static final String COLUMN_WEEKLY_DAYS = "WEEKLY_DAYS";
    private static final String COLUMN_RINGTONE_URI = "ALARM_RINGTONE_URI";
    private static final String COLUMN_RINGTONE_VOLUME = "ALARM_RINGTONE_VOL";
    private static final String COLUMN_IS_VIBR_ON = "IS_VIBR_ON";
    private static final String COLUMN_IS_SNOOZED = "IS_SNOOZED";
    private static final String COLUMN_SNOOZE_LENGTH = "SNOOZE_LENGTH_MINUTES";
    private static final String whereClause = COLUMN_ID + " = ?";

    public DatabaseHelper(@NonNull Application application) {
        super(application.getApplicationContext(), DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE, "
                + COLUMN_NAME + " TEXT UNIQUE,"
                + COLUMN_IS_ACTIVE + " INTEGER,"
                + COLUMN_WEEKLY_DAYS + " TEXT,"
                + COLUMN_ZONEDDATETIME + " TEXT,"
                + COLUMN_RINGTONE_URI + " TEXT,"
                + COLUMN_RINGTONE_VOLUME + " INTEGER,"
                + COLUMN_IS_VIBR_ON + " INTEGER,"
                + COLUMN_IS_SNOOZED + " INTEGER,"
                + COLUMN_SNOOZE_LENGTH + " INTEGER)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    /* returns list of integers already used for default alarm names (i.e. `Alarm 1`) */
    public List<Integer> getUsedDefaultNamesValues() {
        List<Integer> usedValues = new ArrayList<>();
        List<Alarm> alarms = getAllAlarms();
        for (Alarm alarm : alarms) {
            String[] alarmName = alarm.getName().split((" "));
            if (alarmName.length == 2) {
                // if alarm name matches format `Alarm #` it qualifies as `default` style name
                if (Objects.equals(alarmName[0], "Alarm") && Util.isValidInteger(alarmName[1])) {
                    usedValues.add(Integer.parseInt(alarmName[1]));
                }
            }
        }
        return usedValues;
    }

    public String getNextDefaultName() {
        String alarmName = "Alarm";
        List<Integer> usedValues = getUsedDefaultNamesValues();
        int alarmsCount = getAllAlarms().size();
        int nextValue = 1;
        for (int i = 1; i <= alarmsCount + 1; i++) {
            if (!usedValues.contains(i)) {
                nextValue = i;
                break;
            }
        }
        return alarmName + " " + nextValue;
    }

    public void addAlarm(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, alarm.getName());
        cv.put(COLUMN_IS_ACTIVE, alarm.getIsActive() ? 1 : 0);
        cv.put(COLUMN_WEEKLY_DAYS, alarm.weeklySchedToString());
        cv.put(COLUMN_ZONEDDATETIME, String.valueOf(alarm.getZdt()));
        cv.put(COLUMN_RINGTONE_URI, alarm.getRingtoneUri());
        cv.put(COLUMN_RINGTONE_VOLUME, alarm.getRingtoneVol());
        cv.put(COLUMN_IS_VIBR_ON, alarm.getIsVibrOn() ? 1 : 0);
        cv.put(COLUMN_IS_SNOOZED, alarm.getIsSnoozed() ? 1 : 0);
        cv.put(COLUMN_SNOOZE_LENGTH, alarm.getSnoozeLenMins());

        long alarmId = alarm.getId();
        if (alarmId == -1) { // add new alarm to database
            // use database's auto-generated id
            alarmId = db.insertOrThrow(TABLE_NAME, null, cv);
            if (alarmId != -1) alarm.setId(alarmId);
        } else { // update pre-existing alarm
            String[] whereArgs = { String.valueOf(alarmId) };
            db.update(TABLE_NAME, cv, whereClause, whereArgs);
        }
        db.close();
    }

    public void deleteAlarm(long alarmId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, whereClause, new String[]{ String.valueOf(alarmId) });
        db.close();
    }

    public void setActiveStatus(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_ACTIVE, alarm.getIsActive());
        String[] whereArgs = { String.valueOf(alarm.getId()) };
        db.update(TABLE_NAME, cv, whereClause, whereArgs);
        db.close();
    }

    public void setSnoozeStatus(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_SNOOZED, alarm.getIsSnoozed());
        String[] whereArgs = { String.valueOf(alarm.getId()) };
        db.update(TABLE_NAME, cv, whereClause, whereArgs);
        db.close();
    }

    public Alarm getAlarmById(int alarmId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(alarmId) });
        if (cursor.moveToFirst()) {
            return getAlarmFromCursor(cursor);
        }
        cursor.close();
        db.close();
        return null;
    }

    private Alarm getAlarmFromCursor(Cursor cursor) {
        int alarmId = cursor.getInt(0);
        String alarmName = cursor.getString(1);
        int isActive = cursor.getInt(2);
        String weeklySchedStr = cursor.getString(3);
        HashSet<Integer> weeklySched = weeklySchedStr.length() > 0
                ? new HashSet<>(Alarm.weeklySchedStrToList(weeklySchedStr)) : new HashSet<>();
        String alarmZdt = cursor.getString(4);
        ZonedDateTime parsedAlarmZdt = ZonedDateTime.parse(alarmZdt);
        String ringtoneUri = cursor.getString(5);
        int ringtoneVol = cursor.getInt(6);
        int isVibrOn = cursor.getInt(7);
        int isSnoozed = cursor.getInt(8);
        int snoozeLenMins = cursor.getInt(9);
        return new Alarm(alarmName, alarmId, isActive == 1, parsedAlarmZdt, weeklySched, ringtoneUri, ringtoneVol, isVibrOn == 1, isSnoozed == 1, snoozeLenMins);
    }

    public List<Alarm> getAllAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                alarms.add(getAlarmFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return alarms;
    }
}