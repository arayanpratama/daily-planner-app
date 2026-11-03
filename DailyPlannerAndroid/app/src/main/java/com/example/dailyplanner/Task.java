package com.example.dailyplanner;

import java.io.Serializable;
import java.util.Calendar;

public class Task implements Serializable {
    public enum Status { NORMAL, DUE_SOON, OVERDUE, DONE }

    /** Berapa lama sebelum deadline task dianggap "Due soon" dan alarm peringatan berbunyi. */
    public static final long SOON_WINDOW_MS = 2 * 60 * 1000L;

    public String title;
    public String description;
    public String dueTime;
    public boolean completed;
    public long createdAt;
    /** Waktu deadline lengkap (millis). */
    public long dueAt;

    public Task(String title, String description, String dueTime) {
        this.title = title;
        this.description = description;
        this.dueTime = dueTime;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
        this.dueAt = computeDueAt(createdAt, dueTime);
    }

    /**
     * Mengubah "HH:mm" menjadi waktu deadline lengkap: kemunculan berikutnya dari jam tersebut
     * setelah task dibuat (kalau jamnya sudah lewat saat task dibuat, berarti besok).
     */
    public static long computeDueAt(long createdAt, String dueTime) {
        int hour = 9, minute = 0;
        try {
            String[] p = dueTime.split(":");
            hour = Integer.parseInt(p[0].trim());
            minute = Integer.parseInt(p[1].trim());
        } catch (Exception ignored) {}
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(createdAt);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= createdAt) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    public Status status(long now) {
        if (completed) return Status.DONE;
        if (now >= dueAt) return Status.OVERDUE;
        if (dueAt - now <= SOON_WINDOW_MS) return Status.DUE_SOON;
        return Status.NORMAL;
    }
}
