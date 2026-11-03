package com.example.dailyplanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

public class FocusTimerService extends Service {
    public static final String ACTION_START = "com.example.dailyplanner.START_TIMER";
    public static final String ACTION_STOP = "com.example.dailyplanner.STOP_TIMER";
    public static final String ACTION_TICK = "com.example.dailyplanner.TIMER_TICK";
    public static final String EXTRA_SECONDS = "seconds";
    private static final String CHANNEL_ID = "focus_timer_channel";
    private CountDownTimer timer;
    private long remainingMillis = 0;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopTimer();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            long seconds = intent.getLongExtra(EXTRA_SECONDS, 25 * 60);
            if (seconds < 1) seconds = 1;
            startTimer(seconds * 1000L);
        }
        return START_STICKY;
    }

    private void startTimer(long millis) {
        if (timer != null) timer.cancel();
        remainingMillis = millis;
        startForeground(1, notification("Focus session running", format(remainingMillis)));
        timer = new CountDownTimer(millis, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateNotification();
                broadcastTick(false);
            }
            @Override public void onFinish() {
                remainingMillis = 0;
                broadcastTick(true);
                updateNotification();
                stopForeground(true);
                stopSelf();
            }
        }.start();
        broadcastTick(false);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        remainingMillis = 0;
        broadcastTick(true);
    }

    private void broadcastTick(boolean finished) {
        Intent i = new Intent(ACTION_TICK);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_SECONDS, Math.max(0, remainingMillis / 1000));
        i.putExtra("finished", finished);
        sendBroadcast(i);
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(1, notification("Focus session running", format(remainingMillis)));
    }

    private Notification notification(String title, String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle(title).setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(remainingMillis > 0).build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Focus timer", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private String format(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
