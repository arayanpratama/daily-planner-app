DAILY PLANNER - UPDATED ALARM & UI

CHANGES
1. Deadline reminder is scheduled 2 minutes before each task deadline.
2. If a task is created with less than 2 minutes remaining, the warning is scheduled almost immediately.
3. A second alarm notification is scheduled at the deadline.
4. Both notifications use the Android alarm sound and vibration through a high-importance notification channel.
5. Completing a task cancels its pending alarms.
6. Dashboard and task/history pages are scrollable; cards and form controls have cleaner rounded styling.

IMPORTANT ANDROID SETTINGS
- Android 13+: allow notification permission when the app asks.
- Android 12+: allow "Alarms & reminders" / exact alarms if Android opens that settings page.
- Check that notification sound is enabled for the "Task deadline alarms" channel.
- Device volume and Do Not Disturb settings can affect audible alerts.

HOW TO TEST
1. Install/run the app on a real Android phone.
2. Create a task with a deadline about 4 minutes in the future.
3. Keep the task incomplete.
4. About 2 minutes before the deadline, expect a "Due soon" notification with alarm sound/vibration.
5. At the deadline, expect an "Overdue" notification with alarm sound/vibration.
6. Marking the task complete before its deadline cancels its scheduled reminders.

NOTES
- The alert is an Android notification using the system default alarm sound and vibration. It is not a continuously looping siren.
- Exact delivery depends on Android alarm permissions, notification permissions, device settings, and manufacturer battery restrictions.
- The project could not be Gradle-built in this environment because Gradle 9.3.0 could not be downloaded (network unavailable). Open the project folder containing settings.gradle in Android Studio, allow Gradle Sync, then build/run there.
