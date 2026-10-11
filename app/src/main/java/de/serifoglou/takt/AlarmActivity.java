package de.serifoglou.takt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

public class AlarmActivity extends Activity {
    private String id, title, url, tone;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setShowWhenLocked(true); setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        id = getIntent().getStringExtra("id"); title = getIntent().getStringExtra("title");
        url = getIntent().getStringExtra("url"); tone = getIntent().getStringExtra("tone");
        setContentView(buildView());
    }

    private View buildView() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(dp(28),dp(44),dp(28),dp(44)); root.setBackgroundColor(Color.rgb(3,20,38));
        TextView label = text("TAKT · WECKER", 14, Color.rgb(99,234,219)); root.addView(label);
        TextView heading = text(title == null ? "Erinnerung" : title, 30, Color.WHITE); heading.setGravity(Gravity.CENTER); heading.setPadding(0,dp(24),0,dp(42)); root.addView(heading);
        Button done = button("Erledigt"); done.setOnClickListener(v -> done()); root.addView(done, params());
        Button snooze = button("In 10 Minuten erneut erinnern"); snooze.setOnClickListener(v -> snooze()); root.addView(snooze, params());
        return root;
    }

    private void done() {
        stopService(new Intent(this, AlarmService.class).setAction(AlarmService.ACTION_STOP));
        startActivity(new Intent(this, MainActivity.class).putExtra("openUrl", url)); finish();
    }

    private void snooze() {
        try {
            JSONObject data = new JSONObject().put("id", id + ":snooze").put("title", title).put("url", url).put("tone", tone).put("mode", "alarm").put("triggerAt", System.currentTimeMillis()+10*60*1000L);
            AlarmScheduler.schedule(this, data, true);
        } catch (Exception ignored) {}
        stopService(new Intent(this, AlarmService.class).setAction(AlarmService.ACTION_STOP)); finish();
    }

    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextSize(17);return b;}
    private LinearLayout.LayoutParams params(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.setMargins(0,dp(8),0,dp(8));return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
