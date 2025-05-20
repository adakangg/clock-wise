package com.example.alarmclock;

interface RecyclerViewInterface {
    void onItemClick(int position);
    void onItemLongClick(int position);
//    void onPopupClick(int position, View v);
//    void releasePopup(View v);
}

interface DeleteModeInterface {
    void onAlarmClick(int numSelected);
}
