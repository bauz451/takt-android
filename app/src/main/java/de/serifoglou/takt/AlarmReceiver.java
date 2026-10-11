package de.serifoglou.takt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("id");
        context.getSharedPreferences(AlarmScheduler.PREFS, Context.MODE_PRIVATE).edit().remove(id == null ? "" : id).apply();
        scheduleNextDaily(context, intent, id);
        if ("notification".equals(intent.getStringExtra("mode"))) showNotification(context, intent);
        else context.startForegroundService(new Intent(context, AlarmService.class).putExtras(intent));
    }

    private void scheduleNextDaily(Context context, Intent source, String id) {
        if (!source.getBooleanExtra("repeatDaily", false) || id == null) return;
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(source.getLongExtra("triggerAt", System.currentTimeMillis()));
        next.add(Calendar.DAY_OF_MONTH, 1);
        long repeatUntil = source.getLongExtra("repeatUntil", 0L);
        if (repeatUntil > 0 && next.getTimeInMillis() > repeatUntil) return;
        try {
            JSONObject data = new JSONObject()
                    .put("id", id).put("title", source.getStringExtra("title"))
                    .put("url", source.getStringExtra("url")).put("tone", source.getStringExtra("tone"))
                    .put("mode", source.getStringExtra("mode")).put("triggerAt", next.getTimeInMillis())
                    .put("repeatDaily", true).put("repeatUntil", repeatUntil);
            AlarmScheduler.schedule(context, data, true);
        } catch (Exception ignored) {}
    }

    private void showNotification(Context context, Intent source) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        String channelId = "takt_reminders";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(channelId, "Takt Erinnerungen", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true); channel.setDescription("Termine und To-dos");
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(context, MainActivity.class).putExtra("openUrl", source.getStringExtra("url"));
        PendingIntent content = PendingIntent.getActivity(context, source.getStringExtra("id").hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify(source.getStringExtra("id").hashCode(), new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setColor(Color.rgb(99,234,219))
                .setContentTitle("Takt · Erinnerung").setContentText(source.getStringExtra("title"))
                .setPriority(NotificationCompat.PRIORITY_MAX).setVibrate(new long[]{0,450,140,450})
                .setAutoCancel(true).setContentIntent(content).build());
    }
}
