package com.example.alarmclock;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import com.example.alarmclock.utils.Util;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * This activity is used to set the data for a new Alarm or to view/edit the details for a pre-existing Alarm
 */

public class AlarmInfoActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private long alarmId;
    private TextView tvCntdownTitle, tvAlarmDate, tvRingtoneName, tvSnoozeLen, tvVibrStatus, tvCntdownHrsLen, tvCntdownDaysLen, tvCntdownMinsLen, tvCntdownSecsLen;
    private String alarmName;
    private EditText etAlarmName;
    private ZonedDateTime alarmZdt;
    private String[] daysOfWeekAbbrv, amPm;
    private ChipGroup chpGrpDays;
    private HashSet<Integer> selectedDays;
    private boolean changesSaved, isVibrOn, ignoreChipListener;
    private int snoozeLenMins, ringtoneVol;
    private ActivityResultLauncher<Intent> startRingtoneSelectorForResult;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Uri ringtoneUri;
    private CountDownTimer cntdownTimer;
    private NumberPicker hrPicker, amPmPicker;
    private ConstraintLayout layoutCntdown;
    private SwitchCompat swCntdown;

    @SuppressLint({"MissingInflatedId", "UseSwitchCompatOrMaterialCode", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_info);
        databaseHelper = AlarmApplication.getDatabaseHelper();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        Intent alarmIntent = getIntent();
        if (alarmIntent.hasExtra("EXTRA_CLICKED_ALARM_POSITION")) {
            int position = alarmIntent.getIntExtra("EXTRA_CLICKED_ALARM_POSITION", -1);
            loadExistingAlarm(position);
        } else {
            setDefaultAlarmValues();
        }

        setUpAlarmName();
        setupCountdown();
        setupTimePicker();
        setupDatePicker();
        setupZoneSpinner();
        setupSnoozePicker();
        setupVibrationPicker();
        setupRingtoneSelector();
        setupToolbar();
        changesSaved = true;
        Button btnSubmitAlarm = findViewById(R.id.btn_submit);
        btnSubmitAlarm.setOnClickListener(v -> createAlarm());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                        if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                        if (!changesSaved) {
                            showSaveChangesDialog();
                        } else {
                            if (cntdownTimer != null) cntdownTimer.cancel();
                            finish();
                        }
                    }
            );
        }
    }

    private boolean isWeeklyAlarm() { return selectedDays.size() > 0; }

    private void setUpAlarmName() {
        etAlarmName = findViewById(R.id.edit_text_alarm_name);
        etAlarmName.setText(alarmName);
        etAlarmName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) { madeChange(); }
        });

    }

    private void setupTimePicker() {
        amPm = getResources().getStringArray(R.array.am_pm);
        int alarmHr = alarmZdt.getHour();
        hrPicker = findViewById(R.id.picker_hr);
        hrPicker.setFormatter(value -> String.format(Locale.US, "%02d", value));
        hrPicker.setMaxValue(12);
        hrPicker.setMinValue(1);
        int hour12 = (alarmHr == 0 || alarmHr == 12) ? 12 : alarmHr % 12;
        hrPicker.setValue(hour12);
        hrPicker.setOnScrollListener((picker, scrollState) -> {
            handleStillScrolling(scrollState);
            if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                recalcAlarmTime();
            }
        });

        NumberPicker minPicker = findViewById(R.id.picker_min);
        minPicker.setFormatter(value -> String.format(Locale.US, "%02d", value));
        minPicker.setMaxValue(59);
        minPicker.setMinValue(0);
        minPicker.setValue(alarmZdt.getMinute());
        minPicker.setOnScrollListener((picker, scrollState) -> {
            handleStillScrolling(scrollState);
            if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                alarmZdt = alarmZdt.withMinute(picker.getValue());
                if (isWeeklyAlarm()) {
                    alarmZdt = Alarm.calcWeeklyAlarmDate(alarmZdt, selectedDays);
                }
                madeChange();
            }
        });

        amPmPicker = findViewById(R.id.picker_am_pm);
        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(amPm.length-1);
        amPmPicker.setDisplayedValues(amPm);
        int amPmInt = alarmHr < 12 ? 0 : 1;
        amPmPicker.setValue(amPmInt);
        amPmPicker.setOnScrollListener((picker, scrollState) -> {
            handleStillScrolling(scrollState);
            if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                recalcAlarmTime();
            }
        });
    }

    private void handleStillScrolling(int scrollState) {
        if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL ||
                scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_FLING) {
            closeCountdown();
        }
    }

    private void recalcAlarmTime() {
        int hour24 = Util.get24HrFrom12Hr(hrPicker.getValue(), amPm[amPmPicker.getValue()]);
        alarmZdt = alarmZdt.withHour(hour24);
        if (isWeeklyAlarm()) alarmZdt = Alarm.calcWeeklyAlarmDate(alarmZdt, selectedDays);
        madeChange();
    }

    private void setupDatePicker() {
        tvAlarmDate = findViewById(R.id.text_alarm_date);
        ConstraintLayout dateLayout = findViewById(R.id.layout_date_alarm_info);
        dateLayout.setOnClickListener(v -> showDatePicker());
        setupChipGroupDays();

        if (isWeeklyAlarm()) { // set scheduled days for weekly alarms
            HashSet<Integer> selectedChipsCopy = new HashSet<>(selectedDays);
            for (int i : selectedChipsCopy) {
                Chip selectedChip = getChipByTag(chpGrpDays, daysOfWeekAbbrv[i]);
                if (selectedChip != null) selectedChip.setChecked(true);
            }
            tvAlarmDate.setText(R.string.text_weekly);
        }  else {
            tvAlarmDate.setText(Util.formatZdtDate(alarmZdt));
        }
    }


    /* setup chip group used to select days for weekly-repeating alarms */
    private void setupChipGroupDays() {
        daysOfWeekAbbrv = getResources().getStringArray(R.array.days_of_week_abbrv);
        chpGrpDays = findViewById(R.id.chip_group_days);
        chpGrpDays.setChipSpacingHorizontal(10);
        for (int i = 0; i < daysOfWeekAbbrv.length; i++) {
            Chip dayChip = new Chip(new ContextThemeWrapper(this, R.style.CustomChipGroupStyle), null, 0);
            dayChip.setText(daysOfWeekAbbrv[i]);
            dayChip.setTag(daysOfWeekAbbrv[i]);
            dayChip.setCheckable(true);
            dayChip.setTextSize(15);

            // add/remove selected day's index from `selectedDays`
            int iCopy = i;
            dayChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!ignoreChipListener) {
                    if (dayChip.isChecked()) {
                        selectedDays.add(iCopy);
                    } else {
                        if (selectedDays.size() == 1) {
                            dayChip.setChecked(true);
                            Toast.makeText(this, "Must select atleast one date", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedDays.remove(iCopy);
                    }
                    // calculate nearest day matching weekly alarm's selected days
                    alarmZdt = Alarm.calcWeeklyAlarmDate(alarmZdt, selectedDays);
                    tvAlarmDate.setText(R.string.text_weekly);
                    closeCountdown();
                    madeChange();
                }
            });
            chpGrpDays.addView(dayChip);
        }
        ignoreChipListener = false;
    }

    private void clearChipGroupDays() {
        for (int i = 0; i < chpGrpDays.getChildCount(); i++) {
            Chip chip = (Chip) chpGrpDays.getChildAt(i);
            chip.setChecked(false);
        }
        selectedDays.clear();
    }

    private Chip getChipByTag(ChipGroup chipGroup, String chipTag) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chipTag.equals(chip.getTag())) return chip;
            }
        }
        return null;
    }

    private void setupSnoozePicker() {
        ConstraintLayout snoozeLayout = findViewById(R.id.layout_snooze);
        snoozeLayout.setOnClickListener(v -> showSnoozePicker());
        tvSnoozeLen = findViewById(R.id.text_snooze_length);
        SwitchCompat swSnooze = findViewById(R.id.switch_snooze);
        swSnooze.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String snoozeLen;
            if (isChecked) {
                snoozeLenMins = 10;
                snoozeLen = snoozeLenMins + " minutes(s)";
            } else {
                snoozeLenMins = 0;
                snoozeLen = "Off";
            }
            tvSnoozeLen.setText(snoozeLen);
            madeChange();
        });
        swSnooze.setChecked(snoozeLenMins > 0);
        tvSnoozeLen.setText(snoozeLenMins > 0 ? snoozeLenMins + " minute(s)" : "Off");
    }

    private void showSnoozePicker() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.title_dialog);
        tvDialogTitle.setText(R.string.dialog_snooze_title);
        TextView tvDialogMsg = dialogView.findViewById(R.id.text_msg_dialog);
        tvDialogMsg.setVisibility(View.GONE);

        LinearLayout snoozePickerLayout = dialogView.findViewById(R.id.layout_snooze_picker);
        snoozePickerLayout.setVisibility(View.VISIBLE);
        NumberPicker numberPicker = dialogView.findViewById(R.id.number_picker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(60);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    snoozeLenMins = numberPicker.getValue();
                    String snoozeLenStr = snoozeLenMins + " minute(s)";
                    tvSnoozeLen.setText(snoozeLenStr);
                })
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corners_bg);
        dialog.show();
    }

    private void setupVibrationPicker() {
        tvVibrStatus = findViewById(R.id.title_vibration_status);
        SwitchCompat swVibrate = findViewById(R.id.switch_vibrate);
        swVibrate.setChecked(isVibrOn);
        swVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isVibrOn = isChecked;
            tvVibrStatus.setText(isVibrOn ? "On" : "Off");
            madeChange();
        });
        tvVibrStatus.setText(isVibrOn ? "On" : "Off");
    }

    private void setupCountdown() {
        tvCntdownTitle = findViewById(R.id.title_countdown);
        tvCntdownTitle.setVisibility(View.VISIBLE);

        tvCntdownDaysLen = findViewById(R.id.text_countdown_days_length);
        tvCntdownHrsLen = findViewById(R.id.text_countdown_hrs_length);
        tvCntdownMinsLen = findViewById(R.id.text_countdown_mins_length);
        tvCntdownSecsLen = findViewById(R.id.text_countdown_secs_length);
        layoutCntdown = findViewById(R.id.layout_countdown);
        layoutCntdown.setVisibility(View.GONE);

        swCntdown = findViewById(R.id.switch_countdown);
        swCntdown.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (swCntdown.isChecked()) {
                startCountdown();
                tvCntdownTitle.setText(R.string.text_hide_countdown);
                layoutCntdown.setVisibility(View.VISIBLE);
            } else {
                tvCntdownTitle.setText(R.string.text_show_countdown);
                layoutCntdown.setVisibility(View.GONE);
            }
        });
        swCntdown.setChecked(false);
        swCntdown.setVisibility(View.VISIBLE);
    }

    private void setupZoneSpinner() {
        Spinner spnZones = findViewById(R.id.spinner_zones);
        Map<String, String> zonesMap = Util.getTimeZoneMap();
        int alarmZoneIndex = Util.getZoneIndex(zonesMap, alarmZdt.getOffset());
        String[] offsets = zonesMap.keySet().toArray(new String[0]);
        String[] names = zonesMap.values().toArray(new String[0]);
        SpinnerAdapterZones adapter = new SpinnerAdapterZones(this, offsets, names, alarmZoneIndex);
        spnZones.setAdapter(adapter);
        spnZones.setSelection(alarmZoneIndex);

        // delay setting listener to prevent initial selection from triggering spinner's `make change` function
        new Handler(Looper.getMainLooper()).post(() ->
            spnZones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    SpinnerAdapterZones adapter = (SpinnerAdapterZones) parent.getAdapter();
                    adapter.setSelectedPosition(position);
                    ZoneId alarmZoneId = ZoneId.of(offsets[position]);
                    if (isWeeklyAlarm()) {
                        alarmZdt = Alarm.calcWeeklyAlarmDate(alarmZdt.withZoneSameLocal(alarmZoneId), selectedDays);
                    } else {
                        alarmZdt = alarmZdt.withZoneSameLocal(alarmZoneId);
                    }
                    madeChange();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {} // use device's current zone by default
            })
        );
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_alarm_info);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadExistingAlarm(int alarmPosition) {
        Alarm loadedAlarm = databaseHelper.getAllAlarms().get(alarmPosition);
        alarmId = loadedAlarm.getId();
        alarmName = loadedAlarm.getName();
        alarmZdt = loadedAlarm.getZdt();
        selectedDays = loadedAlarm.getWeeklySched();
        ringtoneUri = Uri.parse(loadedAlarm.getRingtoneUri());
        ringtoneVol = loadedAlarm.getRingtoneVol();
        snoozeLenMins = loadedAlarm.getSnoozeLenMins();
        isVibrOn = loadedAlarm.getIsVibrOn();
    }

    private void setDefaultAlarmValues() {
        alarmId = -1;
        alarmName = databaseHelper.getNextDefaultName();
        alarmZdt = ZonedDateTime.now().withSecond(0).withNano(0);
        selectedDays = new HashSet<>();
        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
        ringtoneVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2;
        snoozeLenMins = 10;
        isVibrOn = false;
    }

    private void setupRingtoneSelector() {
        LinearLayout ringtoneLayout = findViewById(R.id.layout_ringtone);
        ringtoneLayout.setOnClickListener(v -> openRingtoneSelector());
        tvRingtoneName = findViewById(R.id.text_ringtone_name);
        mediaPlayer = new MediaPlayer();
        startRingtoneSelectorForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                            setRingtone();
                            madeChange();
                        }
                    }
                }
        );
        setRingtone();
        setupRingtoneVolume();
    }

    private void setRingtone() {
        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        tvRingtoneName.setText(ringtone.getTitle(this));
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(this, ringtoneUri);
            mediaPlayer.prepare();
        } catch (Exception e) {
            Toast.makeText(this, "Media Player Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRingtoneVolume() {
        SeekBar seekbarVol = findViewById(R.id.seekbar_volume);
        seekbarVol.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekbarVol.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)); /* set current volume */
        seekbarVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ringtoneVol = progress;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                madeChange();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { mediaPlayer.start(); }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer.isPlaying()) { mediaPlayer.pause(); }
            }
        });
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ringtoneVol, 0);
    }

    private void openRingtoneSelector() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        startRingtoneSelectorForResult.launch(intent);
    }

    private void startCountdown() { // counts down to current alarm's scheduled time
        long msToAlarm = Duration.between(ZonedDateTime.now(), alarmZdt).toMillis();
        if (cntdownTimer != null) cntdownTimer.cancel();
        cntdownTimer = new CountDownTimer(msToAlarm, 1000) {
            public void onTick(long msToAlarm) {
                long secs = msToAlarm / 1000;
                long mins = secs / 60;
                long hrs = mins / 60;
                long days = hrs / 24;
                tvCntdownDaysLen.setText(String.valueOf(days));
                tvCntdownHrsLen.setText(String.valueOf(hrs % 24));
                tvCntdownMinsLen.setText(String.valueOf(mins % 60));
                tvCntdownSecsLen.setText(String.valueOf(secs % 60));
            }
            public void onFinish() {}
        }.start();
    }

    private void closeCountdown() { if (swCntdown.isChecked()) swCntdown.setChecked(false); }

    private void madeChange() { changesSaved = false; }

    private void showSaveChangesDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.title_dialog);
        tvDialogTitle.setText(R.string.dialog_title);
        TextView tvDialogMsg = dialogView.findViewById(R.id.text_msg_dialog);
        tvDialogMsg.setText(R.string.dialog_changes_msg);
        tvDialogMsg.setVisibility(View.VISIBLE);
        LinearLayout snoozePickerLayout = dialogView.findViewById(R.id.layout_snooze_picker);
        snoozePickerLayout.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    dialog.cancel();
                    createAlarm();
                    finish();
                })
                .setNegativeButton("Discard", (dialog, which) -> {
                    dialog.cancel();
                    finish();
                })
                .setNeutralButton("Cancel", (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corners_bg);
        dialog.show();
    }

    private void showDatePicker() { // used to select dates for 1-time alarms
        int yr = alarmZdt.getYear();
        int mth = alarmZdt.getMonthValue() - 1; // DatePicker's months are 0-indexed VS ZonedDateTime months are 1-indexed
        int day = alarmZdt.getDayOfMonth();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            alarmZdt = alarmZdt.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth);
            tvAlarmDate.setText(Util.formatZdtDate(alarmZdt));
            ignoreChipListener = true;
            clearChipGroupDays(); // don't trigger chip listener function that re-calculates weekly alarm
            ignoreChipListener = false;
            startCountdown();
            madeChange();
        }, yr, mth, day);

        // set minimum date to selected zone's date
        ZoneId alarmZoneId = ZoneId.of(Util.getZoneOffsetName(alarmZdt.getOffset()));
        ZonedDateTime latestZdt =  ZonedDateTime.now(alarmZoneId).withHour(0).withMinute(0).withSecond(0).withNano(0);
        datePicker.getDatePicker().setMinDate(latestZdt.toInstant().toEpochMilli());
        Objects.requireNonNull(datePicker.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_corners_bg);
        datePicker.show();
    }


    /* add alarm to database & set broadcast to trigger notification when alarm goes off */
    private void createAlarm() {
        boolean isValidAlarm = Alarm.verifyAlarm(this, (int) alarmId, alarmZdt, selectedDays);
        if (isValidAlarm) {
            alarmName = etAlarmName.getText().toString();
            Alarm alarm = new Alarm(alarmName, alarmId, true,  alarmZdt, selectedDays, String.valueOf(ringtoneUri), ringtoneVol, isVibrOn, false, snoozeLenMins);
            try {
                databaseHelper.addAlarm(alarm);
            } catch (SQLiteConstraintException e) {
                Toast.makeText(this, "Alarm with same name exists", Toast.LENGTH_LONG).show();
                return;
            } catch (Exception e) {
                Toast.makeText(this, "Error occurred setting alarm", Toast.LENGTH_LONG).show();
                return;
            }
            alarm.setAlarm(this);
            changesSaved = true;
            Toast.makeText(this, "Alarm successfully set", Toast.LENGTH_LONG).show();
            onBackPressed();
        }
    }


    /* close alarm name EditText when user presses anywhere else */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (etAlarmName.hasFocus()) {
                Util.hideKeyboard(etAlarmName);
                etAlarmName.clearFocus();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * @noinspection deprecation
     */
    @Override
    public void onBackPressed() {
        if (mediaPlayer.isPlaying()) mediaPlayer.pause();
        if (!changesSaved) {
            showSaveChangesDialog();
        } else {
            if (cntdownTimer != null) cntdownTimer.cancel();
            super.onBackPressed();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (cntdownTimer != null) cntdownTimer.cancel();
        super.onDestroy();
    }
}