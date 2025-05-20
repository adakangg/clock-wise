package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This fragment displays a scrollable list of all user-created alarms.
 * Uses `RecyclerViewAdapter` to bind alarms data to the RecyclerView.
 * Implements `RecyclerViewInterface` to handle user-interactions with alarms in the RecyclerView.
 * Implements `DeleteModeInterface` to notify `MainActivity` of when alarm `DeleteMode` is entered.
 */

public class AlarmsFragment extends Fragment implements RecyclerViewInterface, DeleteModeInterface {
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter rvAdapter;
    private DeleteModeInterface deleteModeInterface;
    private SharedPreferences preferences;
    private List<Alarm> alarms;
    private HashSet <Integer> selectedAlarms; // holds recyclerview positions of alarms marked for deletion
    private ImageButton btnAddAlarm, btnDeleteAlarm;
    private RadioButton btnSelectAll;
    private boolean inDeleteMode;
    private OnBackPressedCallback callback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof DeleteModeInterface) deleteModeInterface = (DeleteModeInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        preferences = AlarmApplication.getSharedPreferences();
        databaseHelper = AlarmApplication.getDatabaseHelper();
        alarms = databaseHelper.getAllAlarms();
        checkRingingAlarm();
        setupRecyclerView(view);
        setupButtons(view);
        selectedAlarms = new HashSet<>();

        callback = new OnBackPressedCallback(true) {
            /**
             * @noinspection deprecation
             */
            @Override
            public void handleOnBackPressed() {
                if (inDeleteMode) { // ensure back press exits `Delete Mode` without closing app
                    exitDeleteMode();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    public void onBackPress() {
        if (callback != null && callback.isEnabled()) {
            callback.handleOnBackPressed();
        }
    }


    /* open notification page for alarms that were triggered/not dismissed while app was closed */
    private void checkRingingAlarm() {
        int ringingAlarmId = preferences.getInt("PREF_RINGING_ALARM_ID", -1);
        if (ringingAlarmId > -1) {
            Alarm ringingAlarm = databaseHelper.getAlarmById(ringingAlarmId);
            Intent notifIntent = new Intent(requireActivity(), AlarmNotificationActivity.class);
            Bundle notifBundle = new Bundle();
            notifBundle.putParcelable("BUNDLE_RINGING_ALARM", ringingAlarm);
            notifIntent.putExtras(notifBundle);
            notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notifIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(notifIntent);
        }
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_alarms);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), 30, recyclerView.getPaddingRight(), recyclerView.getPaddingBottom());
        setAlarmsList();
    }

    private void setAlarmsList() {
        verifyAlarms();
        rvAdapter = new RecyclerViewAdapter(requireContext(), alarms, this);
        recyclerView.setAdapter(rvAdapter);
    }


    /* ensures passed weekly alarms or invalid (passed 1-time, duplicate) alarms are deactivated */
    private void verifyAlarms() {
        Context context =  AlarmApplication.getInstance().getApplicationContext();
        for (Alarm alarm : alarms) {
            ZonedDateTime alarmZdt = alarm.getZdt();
            if (alarm.isWeeklyAlarm() && !alarmZdt.isAfter(ZonedDateTime.now())) {
                ZonedDateTime updatedZdt = Alarm.calcWeeklyAlarmDate(alarmZdt, alarm.getWeeklySched());
                alarm.setZdt(updatedZdt);
            }
            boolean isValidAlarm = Alarm.verifyAlarm(null, (int) alarm.getId(), alarm.getZdt(), alarm.getWeeklySched());
            if (!isValidAlarm && alarm.getIsActive() && !alarm.getIsSnoozed()) {
                alarm.cancelAlarm(context);
            }
        }
    }

    private void setupButtons(View view) {
        btnAddAlarm = view.findViewById(R.id.btn_add_alarm);
        btnAddAlarm.setOnClickListener(this::openNewAlarmInfoActivity);
        btnAddAlarm.setVisibility(View.VISIBLE);

        btnDeleteAlarm = view.findViewById(R.id.btn_delete_alarm);
        btnDeleteAlarm.setOnClickListener(v -> deleteSelectedAlarms(selectedAlarms));
        btnDeleteAlarm.setVisibility(View.GONE);

        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnSelectAll.setText(R.string.text_select_all);
        btnSelectAll.setVisibility(View.GONE);
        btnSelectAll.setOnClickListener(v -> onSelectAllClick());
    }

    private void onSelectAllClick() {
        if (btnSelectAll.isChecked() && selectedAlarms.size() == alarms.size()) {
            btnSelectAll.setChecked(false);
            selectedAlarms.clear();
            rvAdapter.unselectAllAlarms();
        } else {
            for (int i = 0; i < alarms.size(); i++) {
                selectedAlarms.add(i);
                rvAdapter.selectAlarm(i);
            }
        }
        deleteModeInterface.onAlarmClick(selectedAlarms.size());
    }


    /* makes alarms selectable for deletion */
    private void enterDeleteMode() {
        recyclerView.setPadding(recyclerView.getPaddingLeft(), 6, recyclerView.getPaddingRight(), recyclerView.getPaddingBottom());
        inDeleteMode = true;
        rvAdapter.setInDeleteMode(true);
        btnAddAlarm.setVisibility(View.GONE);
        btnDeleteAlarm.setVisibility(View.VISIBLE);
        btnSelectAll.setVisibility(View.VISIBLE);
        deleteModeInterface.onAlarmClick(selectedAlarms.size());
    }

    private void exitDeleteMode() {
        recyclerView.setPadding(recyclerView.getPaddingLeft(), 30, recyclerView.getPaddingRight(), recyclerView.getPaddingBottom());
        inDeleteMode = false;
        selectedAlarms.clear();
        rvAdapter.setInDeleteMode(false);
        btnAddAlarm.setVisibility(View.VISIBLE);
        btnDeleteAlarm.setVisibility(View.GONE);
        btnSelectAll.setVisibility(View.GONE);
        deleteModeInterface.onAlarmClick(-1); /* signal for `MainActivity` to exit `DeleteMode` & update its UI */
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteSelectedAlarms(HashSet<Integer> selectedAlarms) {
        ArrayList<Integer> alarmsToDelete = new ArrayList<>(selectedAlarms);
        for (int i = alarmsToDelete.size()-1; i >= 0; i--) {
            int position = alarmsToDelete.get(i);
            Alarm alarm = alarms.get(position);
            alarm.cancelAlarm(AlarmApplication.getInstance().getApplicationContext());
            databaseHelper.deleteAlarm(alarm.getId());
            alarms.remove(position);
        }
        rvAdapter.notifyDataSetChanged();
        exitDeleteMode();
    }

    @Override
    public void onItemClick(int position) {
        if (!inDeleteMode) { // open activity to display/edit clicked alarm's information
            Intent newAlarmIntent = new Intent(requireActivity(), AlarmInfoActivity.class);
            newAlarmIntent.putExtra("EXTRA_CLICKED_ALARM_POSITION", position);
            startActivity(newAlarmIntent);
        } else { //select/deselect clicked alarm to mark/unmark for deletion
            if (!selectedAlarms.contains(position)) {
                selectedAlarms.add(position);
            } else {
                selectedAlarms.remove(position);
            }
            deleteModeInterface.onAlarmClick(selectedAlarms.size());
            btnSelectAll.setChecked(selectedAlarms.size() == alarms.size());
            rvAdapter.toggleAlarmSelection(position);
        }
    }


    /* ensures first long-click on an alarm enables `DeleteMode` & marks it for deletion */
    @Override
    public void onItemLongClick(int position) {
        if (!inDeleteMode) {
            if (alarms.size() == 1) btnSelectAll.setChecked(true);
            selectedAlarms.add(position);
            enterDeleteMode();
            rvAdapter.toggleAlarmSelection(position);
        }
    }

    private void openNewAlarmInfoActivity(View v) {
        Intent alarmInfoIntent = new Intent(requireActivity(), AlarmInfoActivity.class);
        startActivity(alarmInfoIntent);
    }

    /*
     * when in `DeleteMode`, communicate the number of selected alarms to `MainActivity`
     * Intentionally left empty here since no action required on Fragment's end
     */
    @Override
    public void onAlarmClick(int numAlarmsSelected) {}


    @Override
    public void onPause() {
        exitDeleteMode();
        super.onPause();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        alarms = databaseHelper.getAllAlarms();
        checkRingingAlarm();
        setAlarmsList();
    }
}