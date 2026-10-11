package de.serifoglou.takt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONObject;
import java.util.Iterator;

public final class AlarmScheduler {
    static final String PREFS = "takt_alarms";
    private AlarmScheduler() {}

    static boolean canSchedule(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
    }

    static boolean schedule(Context context, JSONObject data, boolean persist) {
        long triggerAt = data.optLong("triggerAt", 0L);
        String id = data.optString("id", "");
        if (id.isEmpty() || triggerAt <= System.currentTimeMillis() || !canSchedule(context)) return false;
        Intent intent = new Intent(context, AlarmReceiver.class)
                .putExtra("id", id)
                .putExtra("title", data.optString("title", "Takt-Erinnerung"))
                .putExtra("url", data.optString("url", "https://takt-kalender.bauz-451.chatgpt.site/?reminder=" + id))
                .putExtra("mode", data.optString("mode", "alarm"))
                .putExtra("tone", data.optString("tone", ""))
                .putExtra("triggerAt", triggerAt)
                .putExtra("repeatDaily", data.optBoolean("repeatDaily", false))
                .putExtra("repeatUntil", data.optLong("repeatUntil", 0L));
        PendingIntent pending = PendingIntent.getBroadcast(context, requestCode(id), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, pending), pending);
        if (persist) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(id, data.toString()).apply();
        return true;
    }

    static void cancel(Context context, String id) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, requestCode(id), intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(pending);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(id).apply();
    }

    static void restore(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String json : prefs.getAll().values().stream().map(String::valueOf).toArray(String[]::new)) {
            try {
                JSONObject data = new JSONObject(json);
                if (data.optLong("triggerAt", 0L) > System.currentTimeMillis()) schedule(context, data, false);
                else prefs.edit().remove(data.optString("id")).apply();
            } catch (Exception ignored) {}
        }
    }

    private static int requestCode(String id) { return id.hashCode() & 0x7fffffff; }
}
