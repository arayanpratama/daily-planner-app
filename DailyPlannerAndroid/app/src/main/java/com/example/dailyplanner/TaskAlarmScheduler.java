package com.example.dailyplanner;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

/**
 * Menjadwalkan 2 alarm per task lewat AlarmManager (tetap bunyi walau app ditutup):
 *  - KIND_WARNING  : SOON_WINDOW sebelum deadline  -> "Due soon"
 *  - KIND_DEADLINE : tepat saat deadline            -> "Overdue"
 */
public final class TaskAlarmScheduler {
    public static final String ACTION_TASK_ALARM = "com.example.dailyplanner.TASK_ALARM";
    public static final String EXTRA_CREATED_AT = "createdAt";
    public static final String EXTRA_KIND = "kind";
    public static final int KIND_WARNING = 0;
    public static final int KIND_DEADLINE = 1;

    // Channel ID baru agar pengaturan suara alarm versi lama tidak terus dipakai Android.
    private static final String CHANNEL_ID = "task_deadline_alarm_channel_v2";
    private static final long[] VIBRATION = {0, 500, 250, 500, 250, 500};

    private TaskAlarmScheduler() {}

    private static int requestCode(long createdAt, int kind) {
        return (int) (createdAt & 0x3FFFFFFFL) * 2 + kind;
    }

    /** Pasang ulang alarm sebuah task (dan batalkan kalau sudah selesai). */
    public static void schedule(Context context, Task task) {
        cancelAlarms(context, task);
        if (task.completed) {
            cancelNotifications(context, task);
            return;
        }
        long now = System.currentTimeMillis();
        if (task.dueAt > now) {
            // Jika deadline masih lebih dari 2 menit, ingatkan tepat 2 menit sebelumnya.
            // Jika task dibuat saat sisa waktunya kurang dari 2 menit, ingatkan segera.
            long warnAt = Math.max(now + 1000L, task.dueAt - Task.SOON_WINDOW_MS);
            setAlarm(context, task.createdAt, KIND_WARNING, warnAt);
            setAlarm(context, task.createdAt, KIND_DEADLINE, task.dueAt);
        }
    }

    public static void rescheduleAll(Context context) {
        for (Task t : TaskStore.getTasks(context)) schedule(context, t);
    }

    private static PendingIntent pendingIntent(Context context, long createdAt, int kind, int flags) {
        Intent i = new Intent(context, TaskAlarmReceiver.class);
        i.setAction(ACTION_TASK_ALARM);
        i.putExtra(EXTRA_CREATED_AT, createdAt);
        i.putExtra(EXTRA_KIND, kind);
        return PendingIntent.getBroadcast(context, requestCode(createdAt, kind), i,
                flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(Context context, long createdAt, int kind, long triggerAt) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = pendingIntent(context, createdAt, kind, PendingIntent.FLAG_UPDATE_CURRENT);
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private static void cancelAlarms(Context context, Task task) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        for (int kind = KIND_WARNING; kind <= KIND_DEADLINE; kind++) {
            PendingIntent pi = pendingIntent(context, task.createdAt, kind, PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    private static void cancelNotifications(Context context, Task task) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        for (int kind = KIND_WARNING; kind <= KIND_DEADLINE; kind++) {
            nm.cancel(requestCode(task.createdAt, kind));
        }
    }

    private static Uri alarmSound() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return uri;
    }

    private static AudioAttributes alarmAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Task deadline alarms", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Sound and vibration reminders 2 minutes before and at task deadlines");
        channel.setSound(alarmSound(), alarmAttributes());
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATION);
        nm.createNotificationChannel(channel);
    }

    /** Menampilkan notifikasi + suara alarm untuk sebuah task. */
    public static void notifyTask(Context context, Task task, int kind) {
        ensureChannel(context);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int code = requestCode(task.createdAt, kind);
        PendingIntent content = PendingIntent.getActivity(context, code, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean deadline = kind == KIND_DEADLINE;
        String title = (deadline ? "Overdue: " : "Due soon: ") + task.title;
        String text = deadline
                ? "Deadline " + task.dueTime + " has passed. Complete the task or mark it done."
                : "Only 2 minutes or less until " + task.dueTime + ". Get ready to finish!";

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setSound(alarmSound(), alarmAttributes())
                    .setVibrate(VIBRATION)
                    .setPriority(Notification.PRIORITY_HIGH);
        }
        nm.notify(code, b.build());
    }
}
