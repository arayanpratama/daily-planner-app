package com.example.dailyplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !TaskAlarmScheduler.ACTION_TASK_ALARM.equals(intent.getAction())) return;
        long createdAt = intent.getLongExtra(TaskAlarmScheduler.EXTRA_CREATED_AT, -1);
        int kind = intent.getIntExtra(TaskAlarmScheduler.EXTRA_KIND, TaskAlarmScheduler.KIND_DEADLINE);
        for (Task t : TaskStore.getTasks(context)) {
            if (t.createdAt == createdAt) {
                if (!t.completed) TaskAlarmScheduler.notifyTask(context, t, kind);
                return;
            }
        }
    }
}
