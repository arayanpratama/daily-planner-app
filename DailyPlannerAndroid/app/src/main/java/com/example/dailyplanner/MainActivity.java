package com.example.dailyplanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int ADD_TASK_REQUEST = 100;
    private FrameLayout content;
    private LinearLayout root;
    private TextView timerLabel;
    private long timerSeconds = 25 * 60;
    private boolean timerRunning = false;
    private BroadcastReceiver timerReceiver;
    private String currentSection = "dashboard";
    private String lastSignature = "";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    // Cek tiap 5 detik: kalau ada task yang statusnya berubah (mis. jadi Overdue), tampilan di-refresh.
    private final Runnable statusTicker = new Runnable() {
        @Override public void run() {
            if (!taskSignature().equals(lastSignature)) showSection(currentSection);
            uiHandler.postDelayed(this, 5000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }
        TaskAlarmScheduler.ensureChannel(this);
        buildLayout();
        showSection("dashboard");
        timerReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                timerSeconds = intent.getLongExtra(FocusTimerService.EXTRA_SECONDS, 0);
                timerRunning = timerSeconds > 0 && !intent.getBooleanExtra("finished", false);
                updateTimerLabel();
                if (intent.getBooleanExtra("finished", false)) {
                    Toast.makeText(MainActivity.this, "Focus session complete!", Toast.LENGTH_LONG).show();
                }
            }
        };
        IntentFilter filter = new IntentFilter(FocusTimerService.ACTION_TICK);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(timerReceiver, filter);
    }

    private void buildLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF4F6FB);
        root.setPadding(dp(20), dp(22), dp(20), dp(8));
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL);
        TextView brand = text("DAILY PLANNER  /  YOUR PERSONAL SPACE", 11, 0xFF5965D8, true);
        TextView heading = text("Plan your day.", 30, 0xFF172033, true);
        TextView subtitle = text("Make time for what matters.", 14, 0xFF788197, false);
        header.addView(brand);
        LinearLayout.LayoutParams hp = matchWrap(); hp.topMargin = dp(5);
        header.addView(heading, hp);
        LinearLayout.LayoutParams sp = matchWrap(); sp.topMargin = dp(3);
        header.addView(subtitle, sp);
        root.addView(header, matchWrap());

        content = new FrameLayout(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1f); cp.topMargin = dp(18); cp.bottomMargin = dp(8);
        root.addView(content, cp);

        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(0xFFFFFFFF);
        String[][] tabs = {{"⌂", "Home", "dashboard"}, {"☷", "Tasks", "tasks"}, {"✓", "History", "history"}};
        for (String[] tab : tabs) {
            Button b = new Button(this); b.setText(tab[0] + "\n" + tab[1]); b.setAllCaps(false);
            b.setTextSize(12); b.setOnClickListener(v -> showSection(tab[2]));
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        root.addView(nav, matchWrap());
        setContentView(root);
    }

    private void showSection(String section) {
        currentSection = section;
        lastSignature = taskSignature();
        android.app.Fragment fragment;
        if ("tasks".equals(section)) fragment = new TaskListFragment();
        else if ("history".equals(section)) fragment = new HistoryFragment();
        else fragment = new DashboardFragment();
        getFragmentManager().beginTransaction()
                .replace(content.getId() == View.NO_ID ? ensureContentId() : content.getId(), fragment)
                .commit();
    }

    private int ensureContentId() {
        content.setId(View.generateViewId());
        return content.getId();
    }

    public View buildDashboard() {
        LinearLayout page = vertical();
        ArrayList<Task> tasks = TaskStore.getTasks(this);
        int completed = 0;
        for (Task t : tasks) if (t.completed) completed++;
        LinearLayout summary = card();
        summary.addView(text("YOUR OVERVIEW", 12, 0xFF77798B, true));
        summary.addView(text(tasks.size() + " tasks planned", 23, 0xFF191B2A, true));
        summary.addView(text(completed + " completed  ·  " + (tasks.size()-completed) + " remaining", 14, 0xFF77798B, false));
        page.addView(summary, matchWrap());

        LinearLayout timer = card(); LinearLayout.LayoutParams tp = matchWrap(); tp.topMargin = dp(14);
        timer.addView(text("FOCUS TIMER", 12, 0xFF77798B, true));
        timerLabel = text(format(timerSeconds), 42, 0xFF5B5FEF, true);
        LinearLayout.LayoutParams tlp = matchWrap(); tlp.topMargin = dp(4);
        timer.addView(timerLabel, tlp);
        timer.addView(text("A little focus goes a long way.", 14, 0xFF77798B, false));
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL);
        Button start = new Button(this); start.setText("Start 25 min");
        start.setOnClickListener(v -> {
            if (timerRunning) { Toast.makeText(this, "Timer is already running", Toast.LENGTH_SHORT).show(); return; }
            Intent i = new Intent(this, FocusTimerService.class);
            i.setAction(FocusTimerService.ACTION_START); i.putExtra(FocusTimerService.EXTRA_SECONDS, 25 * 60L);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            timerRunning = true; timerSeconds = 25 * 60; updateTimerLabel();
        });
        Button stop = new Button(this); stop.setText("Stop");
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, FocusTimerService.class); i.setAction(FocusTimerService.ACTION_STOP); startService(i);
            timerRunning = false; timerSeconds = 25 * 60; updateTimerLabel();
        });
        controls.addView(start, new LinearLayout.LayoutParams(0, -2, 1)); controls.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
        timer.addView(controls); page.addView(timer, tp);

        Button add = new Button(this); add.setText("+  Add a new task");
        LinearLayout.LayoutParams ap = matchWrap(); ap.topMargin = dp(14);
        page.addView(add, ap);
        add.setOnClickListener(v -> startActivityForResult(new Intent(this, AddTaskActivity.class), ADD_TASK_REQUEST));

        TextView upcoming = text("UP NEXT", 12, 0xFF77798B, true);
        LinearLayout.LayoutParams up = matchWrap(); up.topMargin = dp(20); up.bottomMargin = dp(8);
        page.addView(upcoming, up);
        int shown = 0;
        for (Task t : tasks) {
            if (t.completed) continue;
            page.addView(taskCard(t, indexOfTask(t)));
            shown++;
            if (shown == 3) break;
        }
        if (shown == 0) page.addView(emptyState("No tasks yet", "Add your first task to get started."));
        return scrollPage(page);
    }

    public View buildTaskList(boolean history) {
        LinearLayout page = vertical();
        TextView title = text(history ? "Completed tasks" : "All tasks", 22, 0xFF191B2A, true);
        page.addView(title);
        TextView desc = text(history ? "Your progress, one task at a time." : "Everything you need to get done.", 14, 0xFF77798B, false);
        LinearLayout.LayoutParams dp = matchWrap(); dp.topMargin = dp(4); dp.bottomMargin = dp(14);
        page.addView(desc, dp);
        ArrayList<Task> tasks = TaskStore.getTasks(this);
        int count = 0;
        for (Task t : tasks) {
            if (history != t.completed) continue;
            page.addView(taskCard(t, indexOfTask(t))); count++;
        }
        if (count == 0) page.addView(emptyState(history ? "Nothing completed yet" : "Your task list is clear", history ? "Completed tasks will appear here." : "Tap Home to add your first task."));
        if (!history) {
            Button add = new Button(this); add.setText("+  Add a new task");
            LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(12); page.addView(add, p);
            add.setOnClickListener(v -> startActivityForResult(new Intent(this, AddTaskActivity.class), ADD_TASK_REQUEST));
        }
        return scrollPage(page);
    }

    private View taskCard(Task task, int index) {
        Task.Status status = task.status(System.currentTimeMillis());
        LinearLayout c = card();
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        Button check = new Button(this); check.setText(task.completed ? "✓" : "○"); check.setMinWidth(dp(44));
        check.setOnClickListener(v -> {
            task.completed = !task.completed;
            TaskStore.updateTask(this, index, task);
            TaskAlarmScheduler.schedule(this, task); // selesai -> alarm dibatalkan, dibuka lagi -> dijadwalkan ulang
            showSection("tasks");
        });
        c.addView(check);
        LinearLayout details = vertical();

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL); titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(task.title, 16, 0xFF191B2A, true);
        if (task.completed) title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = statusBadge(status);
        if (badge != null) {
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2); bp.leftMargin = dp(8);
            titleRow.addView(badge, bp);
        }
        details.addView(titleRow);

        if (!task.description.isEmpty()) {
            TextView description = text(task.description, 13, 0xFF788197, false);
            LinearLayout.LayoutParams descParams = matchWrap();
            descParams.topMargin = dp(4);
            details.addView(description, descParams);
        }
        int dueColor = status == Task.Status.OVERDUE ? 0xFFD12B3A
                : status == Task.Status.DUE_SOON ? 0xFFC76A00 : 0xFF5B5FEF;
        TextView due = text((status == Task.Status.OVERDUE ? "DEADLINE PASSED  ·  " : "DUE  ·  ") + task.dueTime,
                11, dueColor, true);
        LinearLayout.LayoutParams dueParams = matchWrap();
        dueParams.topMargin = dp(7);
        details.addView(due, dueParams);
        c.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        return c;
    }

    /** Label kecil di samping judul task: Overdue / Due soon / Done. Null kalau status normal. */
    private TextView statusBadge(Task.Status status) {
        String label; int fg; int bg;
        switch (status) {
            case OVERDUE:  label = "Overdue";  fg = 0xFFD12B3A; bg = 0xFFFDECEC; break;
            case DUE_SOON: label = "Due soon"; fg = 0xFFC76A00; bg = 0xFFFFF3E0; break;
            case DONE:     label = "Done";     fg = 0xFF1E8E4E; bg = 0xFFE6F6EC; break;
            default: return null;
        }
        TextView t = text(label, 11, fg, true);
        t.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(bg); shape.setCornerRadius(dp(10));
        t.setBackground(shape);
        return t;
    }

    private String taskSignature() {
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (Task t : TaskStore.getTasks(this)) sb.append(t.status(now).ordinal());
        return sb.toString();
    }

    private void promptExactAlarmIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        AlarmManager am = getSystemService(AlarmManager.class);
        SharedPreferences prefs = getSharedPreferences("daily_planner_settings", MODE_PRIVATE);
        if (am == null || am.canScheduleExactAlarms() || prefs.getBoolean("asked_exact_alarm", false)) return;
        prefs.edit().putBoolean("asked_exact_alarm", true).apply();
        Toast.makeText(this, "Allow \"Alarms & reminders\" so deadline alarms ring on time", Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
        } catch (Exception ignored) {}
    }

    private View emptyState(String title, String subtitle) {
        LinearLayout c = card(); c.setGravity(Gravity.CENTER); c.setOrientation(LinearLayout.VERTICAL);
        c.addView(text("✦", 28, 0xFF5B5FEF, true));
        c.addView(text(title, 17, 0xFF191B2A, true));
        c.addView(text(subtitle, 13, 0xFF77798B, false));
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(17), dp(16), dp(17), dp(16));
        c.setBackground(makeRounded(0xFFFFFFFF, 18));
        c.setElevation(dp(2));
        LinearLayout.LayoutParams p = matchWrap();
        p.bottomMargin = dp(12);
        c.setLayoutParams(p);
        return c;
    }

    private ScrollView scrollPage(View page) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(8));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private GradientDrawable makeRounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD); return t;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private int indexOfTask(Task target) {
        ArrayList<Task> tasks = TaskStore.getTasks(this);
        for (int i = 0; i < tasks.size(); i++) if (tasks.get(i).createdAt == target.createdAt && tasks.get(i).title.equals(target.title)) return i;
        return -1;
    }
    private String format(long seconds) {
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }
    private void updateTimerLabel() { if (timerLabel != null) timerLabel.setText(format(timerSeconds)); }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_TASK_REQUEST && resultCode == RESULT_OK && data != null) {
            Task task = new Task(data.getStringExtra("title"), data.getStringExtra("description"), data.getStringExtra("dueTime"));
            TaskStore.addTask(this, task);
            TaskAlarmScheduler.schedule(this, task);
            showSection("dashboard");
            Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
            promptExactAlarmIfNeeded();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        TaskAlarmScheduler.rescheduleAll(this); // pasang ulang (mis. setelah izin alarm diberikan)
        uiHandler.removeCallbacks(statusTicker);
        uiHandler.post(statusTicker);
    }

    @Override protected void onPause() {
        uiHandler.removeCallbacks(statusTicker);
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (timerReceiver != null) try { unregisterReceiver(timerReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
