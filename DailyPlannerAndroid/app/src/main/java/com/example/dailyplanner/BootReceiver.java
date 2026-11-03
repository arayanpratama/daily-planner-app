package com.example.dailyplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm hilang saat HP restart, jadi dipasang ulang di sini. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            TaskAlarmScheduler.rescheduleAll(context);
        }
    }
}
