package com.example.dailyplanner;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends Activity {
    private EditText titleInput, descriptionInput;
    private TextView timeValue;
    private String dueTime = "09:00";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        root.setBackgroundColor(0xFFF4F6FB);

        TextView heading = text("Create a task", 28, 0xFF191B2A, true);
        root.addView(heading, matchWrap());
        TextView subtitle = text("Add something you want to get done.", 14, 0xFF77798B, false);
        LinearLayout.LayoutParams sub = matchWrap(); sub.topMargin = dp(6); sub.bottomMargin = dp(24);
        root.addView(subtitle, sub);

        root.addView(label("Task title"));
        titleInput = new EditText(this);
        titleInput.setSingleLine(true); titleInput.setHint("e.g. Finish assignment");
        styleInput(titleInput);
        root.addView(titleInput, fieldParams());

        root.addView(label("Description (optional)"));
        descriptionInput = new EditText(this);
        descriptionInput.setHint("Add a few details"); descriptionInput.setMinLines(2);
        descriptionInput.setGravity(Gravity.TOP);
        styleInput(descriptionInput);
        root.addView(descriptionInput, fieldParams());

        root.addView(label("Due time"));
        Button timeButton = new Button(this);
        timeButton.setText("Choose time  ·  " + dueTime);
        timeButton.setAllCaps(false);
        timeButton.setTextColor(0xFF4F5CCB);
        timeButton.setBackground(buttonBackground(0xFFE9ECFF));
        timeButton.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                dueTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                timeButton.setText("Choose time  ·  " + dueTime);
        timeButton.setAllCaps(false);
        timeButton.setTextColor(0xFF4F5CCB);
        timeButton.setBackground(buttonBackground(0xFFE9ECFF));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
        root.addView(timeButton, matchWrap());

        Button save = new Button(this);
        save.setText("Save task");
        save.setAllCaps(false);
        save.setTextColor(0xFFFFFFFF);
        save.setBackground(buttonBackground(0xFF5965D8));
        LinearLayout.LayoutParams saveParams = matchWrap(); saveParams.topMargin = dp(24);
        root.addView(save, saveParams);
        save.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            if (title.isEmpty()) {
                titleInput.setError("Task title is required");
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent result = new Intent();
            result.putExtra("title", title);
            result.putExtra("description", descriptionInput.getText().toString().trim());
            result.putExtra("dueTime", dueTime);
            setResult(RESULT_OK, result);
            finish();
        });

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        cancel.setTextColor(0xFF596273);
        cancel.setBackground(buttonBackground(0xFFE9ECF2));
        root.addView(cancel, matchWrap());
        cancel.setOnClickListener(v -> finish());
        setContentView(root);
    }

    private TextView label(String value) {
        TextView t = text(value, 14, 0xFF45475A, true);
        LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(16); p.bottomMargin = dp(4);
        t.setLayoutParams(p); return t;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }
    private void styleInput(EditText e) {
        e.setTextSize(16);
        e.setTextColor(0xFF172033);
        e.setHintTextColor(0xFF9AA3B5);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFFE4E8F0);
        e.setBackground(bg);
    }
    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams p = matchWrap(); p.topMargin = dp(6); return p;
    }
    private android.graphics.drawable.GradientDrawable buttonBackground(int color) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        return bg;
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
}
