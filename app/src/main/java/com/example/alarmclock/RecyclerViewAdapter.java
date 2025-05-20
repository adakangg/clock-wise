package com.example.alarmclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.alarmclock.utils.Util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

/**
 * This adapter is used by `AlarmsFragment` to bind/format alarm data for each item in the RecyclerView.
 */

@SuppressLint({"NotifyDataSetChanged"})
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    private final Context context;
    private final RecyclerViewInterface recyclerViewInterface;
    private final List<Alarm> alarms;
    private final HashSet<Integer> selectedAlarms = new HashSet<>();
    private boolean inDeleteMode;

    public RecyclerViewAdapter(Context context, List<Alarm> alarms, RecyclerViewInterface recyclerViewInterface) {
        this.context = context;
        this.alarms = alarms;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_alarm, parent,false);
        return new MyViewHolder(view, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Alarm alarm = alarms.get(position);
        holder.tvAlarmName.setText(alarm.getName());

        String alarmDate = alarm.isWeeklyAlarm() ?
                alarm.weeklySchedToAbbrev(alarm.getWeeklySched(), "  ") : Util.formatZdtDate(alarm.getZdt());
        holder.tvAlarmDate.setText(alarmDate);

        ZonedDateTime alarmZdt = alarm.getZdt();
        holder.tvAlarmZone.setText(Util.formatOffset(alarmZdt.getOffset()));
        formatAlarmTime(alarmZdt, holder);

        holder.swAlarmActive.setChecked(alarm.getIsActive());
        holder.swAlarmActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                activateAlarm(alarm, alarmZdt, holder);
            } else {
                if (alarm.getIsActive()) {
                    alarm.cancelAlarm(context);
                    holder.tvSnoozed.setVisibility(View.INVISIBLE);
                }
            }
        });
        int showSnooze = alarm.getIsSnoozed() ? View.VISIBLE : View.INVISIBLE;
        holder.tvSnoozed.setVisibility(showSnooze);

        if (inDeleteMode) {
            setDeleteModeLayout(position, holder);
        } else {
            setDefaultLayout(holder);
        }
    }

    private void formatAlarmTime(ZonedDateTime alarmZdt, MyViewHolder holder) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm");
        DateTimeFormatter amPmFormatter = DateTimeFormatter.ofPattern("a");
        holder.tvAlarmTime.setText(alarmZdt.format(timeFormatter));
        holder.tvAlarmAM_PM.setText(alarmZdt.format(amPmFormatter));
    }

    private void activateAlarm(Alarm alarm, ZonedDateTime alarmZdt, MyViewHolder holder) {
        if (!alarm.getIsActive()) {
            if (alarm.isWeeklyAlarm()) {
                alarm.setZdt(Alarm.calcWeeklyAlarmDate(alarmZdt, alarm.getWeeklySched()));
            }
            boolean isValidAlarm = Alarm.verifyAlarm(context, (int) alarm.getId(), alarmZdt, alarm.getWeeklySched());
            if (!isValidAlarm) {
                holder.swAlarmActive.setChecked(false);
            } else {
                alarm.setAlarm(context);
            }
        }
    }

    private void setDefaultLayout(MyViewHolder holder) {
        holder.btnSelectAlarm.setVisibility(View.GONE);
        holder.swAlarmActive.setVisibility(View.VISIBLE);
        holder.timeLayoutParams.setMarginStart(holder.timeLeftMargin);
        holder.timeLayout.setLayoutParams(holder.timeLayoutParams);
        selectedAlarms.clear();
    }

    private void setDeleteModeLayout(int position, MyViewHolder holder) {
        holder.btnSelectAlarm.setVisibility(View.VISIBLE);
        holder.swAlarmActive.setVisibility(View.GONE);
        holder.timeLayoutParams.setMarginStart(holder.timeLeftMargin + 80);
        holder.timeLayout.setLayoutParams(holder.timeLayoutParams);
        if (selectedAlarms.contains(position)) {
            holder.btnSelectAlarm.setChecked(true);
        } else if (selectedAlarms.size() == 0) {
            holder.btnSelectAlarm.setChecked(false);
        }
    }

    public void setInDeleteMode(boolean inDeleteMode) {
        this.inDeleteMode = inDeleteMode;
        notifyDataSetChanged();
    }

    public void toggleAlarmSelection(int position) {
        if (selectedAlarms.contains(position)) {
            selectedAlarms.remove(position);
        } else {
            selectedAlarms.add(position);
        }
        notifyItemChanged(position);
    }

    public void selectAlarm(int position) {
        selectedAlarms.add(position);
        notifyItemChanged(position);
    }

    public void unselectAllAlarms() {
        selectedAlarms.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    /**
     * This class is used to setup the layout for the `RecyclerViewAdapter`.
     * Implements `RecyclerViewInterface` methods to handle user interactions for each alarm item in the `RecyclerView`.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        View timeLayout;
        ViewGroup.MarginLayoutParams timeLayoutParams;
        TextView tvAlarmName, tvAlarmTime, tvAlarmDate, tvAlarmZone, tvAlarmAM_PM, tvSnoozed;
        RadioButton btnSelectAlarm;
        SwitchCompat swAlarmActive;
        int timeLeftMargin;

        @SuppressLint("ClickableViewAccessibility")
        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recycleViewInterface) {
            super(itemView);
            tvAlarmName = itemView.findViewById(R.id.text_alarm_name_cv);
            tvAlarmTime = itemView.findViewById(R.id.text_alarm_time_cv);
            tvAlarmAM_PM = itemView.findViewById(R.id.text_am_pm_cv);
            tvAlarmDate = itemView.findViewById(R.id.text_alarm_date_cv);
            tvAlarmZone = itemView.findViewById(R.id.text_alarm_zone_cv);
            tvSnoozed = itemView.findViewById(R.id.text_snoozed_cv);
            swAlarmActive = itemView.findViewById(R.id.switch_theme);
            btnSelectAlarm = itemView.findViewById(R.id.btn_select_alarm_cv);
            btnSelectAlarm.setVisibility(View.GONE);
            timeLayout =  itemView.findViewById(R.id.layout_time_cv);
            timeLayoutParams =  (ViewGroup.MarginLayoutParams) timeLayout.getLayoutParams();
            timeLeftMargin = timeLayoutParams.getMarginStart();

            itemView.setOnClickListener(v -> {
                if (recycleViewInterface != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) recycleViewInterface.onItemClick(position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (recycleViewInterface != null) {
                    int position = getAdapterPosition();
                    if (getAdapterPosition() != RecyclerView.NO_POSITION) recycleViewInterface.onItemLongClick(position);
                }
                return true;
            });
        }
    }
}