package com.example.dailyplanner;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TaskStore {
    private static final String PREFS = "daily_planner_data";
    private static final String KEY = "tasks";
    private TaskStore() {}

    public static synchronized ArrayList<Task> getTasks(Context context) {
        ArrayList<Task> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Task t = new Task(o.optString("title"), o.optString("description"), o.optString("dueTime"));
                t.completed = o.optBoolean("completed", false);
                t.createdAt = o.optLong("createdAt", System.currentTimeMillis());
                long dueAt = o.optLong("dueAt", 0);
                t.dueAt = dueAt > 0 ? dueAt : Task.computeDueAt(t.createdAt, t.dueTime);
                result.add(t);
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static synchronized void saveTasks(Context context, List<Task> tasks) {
        JSONArray array = new JSONArray();
        try {
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("title", t.title);
                o.put("description", t.description);
                o.put("dueTime", t.dueTime);
                o.put("completed", t.completed);
                o.put("createdAt", t.createdAt);
                o.put("dueAt", t.dueAt);
                array.put(o);
            }
        } catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply();
    }

    public static synchronized void addTask(Context context, Task task) {
        ArrayList<Task> tasks = getTasks(context);
        tasks.add(task);
        saveTasks(context, tasks);
    }

    public static synchronized void updateTask(Context context, int index, Task task) {
        ArrayList<Task> tasks = getTasks(context);
        if (index >= 0 && index < tasks.size()) {
            tasks.set(index, task);
            saveTasks(context, tasks);
        }
    }
}
