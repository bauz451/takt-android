package de.serifoglou.takt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {
    public static final String ACTION_STOP = "de.serifoglou.takt.STOP_ALARM";
    private MediaPlayer player;
    private Vibrator vibrator;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) { stopAlarm(); return START_NOT_STICKY; }
        String id = intent.getStringExtra("id"), title = intent.getStringExtra("title");
        createChannel();
        Intent fullIntent = new Intent(this, AlarmActivity.class).putExtras(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(this, id.hashCode(), fullIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, "takt_alarm_active")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Takt · Wecker")
                .setContentText(title).setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true).setFullScreenIntent(fullScreen, true).setContentIntent(fullScreen).build();
        startForeground(7001, notification);
        play(intent.getStringExtra("tone"));
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,700,250,700,250}, 0));
        startActivity(fullIntent);
        return START_STICKY;
    }

    private void play(String tone) {
        try {
            Uri uri = tone == null || tone.isEmpty() ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : Uri.parse(tone);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this, uri); player.setLooping(true); player.prepare(); player.start();
        } catch (Exception ignored) {}
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel("takt_alarm_active", "Aktive Takt-Wecker", NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null); channel.enableVibration(false); channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void stopAlarm() {
        cleanup();
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }

    private void cleanup() {
        if (player != null) { player.stop(); player.release(); player = null; }
        if (vibrator != null) vibrator.cancel();
    }

    @Override public void onDestroy() { cleanup(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
