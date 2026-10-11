package de.serifoglou.takt;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://takt-kalender.bauz-451.chatgpt.site";
    private static final int PICK_TONE = 41;
    private WebView webView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); requestNotificationPermission(); setContentView(buildView());
        String url=getIntent().getStringExtra("openUrl"); webView.loadUrl(safeAppUrl(url));
    }

    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); String url=intent.getStringExtra("openUrl"); webView.loadUrl(safeAppUrl(url)); }

    private LinearLayout buildView() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(3,20,38));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(14),0,dp(8),0);
        TextView name=new TextView(this);name.setText("Takt");name.setTextColor(Color.WHITE);name.setTextSize(20);name.setTypeface(null,1);bar.addView(name,new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(52)));
        webView=new WebView(this);webView.getSettings().setJavaScriptEnabled(true);webView.getSettings().setDomStorageEnabled(true);webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){return openSafely(request.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return openSafely(Uri.parse(url));}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){
                Uri uri=Uri.parse(url);view.removeJavascriptInterface("TaktAndroid");
                if(isAppHost(uri))view.addJavascriptInterface(new TaktBridge(),"TaktAndroid");
                super.onPageStarted(view,url,icon);
            }
        });webView.setWebChromeClient(new WebChromeClient());webView.addJavascriptInterface(new TaktBridge(),"TaktAndroid");
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));return root;
    }

    private boolean openSafely(Uri uri){
        if(isAppHost(uri)||isAuthHost(uri))return false;
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){Toast.makeText(this,"Link konnte nicht geöffnet werden.",Toast.LENGTH_SHORT).show();}
        return true;
    }

    private boolean isAppHost(Uri uri){return "https".equals(uri.getScheme())&&"takt-kalender.bauz-451.chatgpt.site".equals(uri.getHost());}
    private boolean isAuthHost(Uri uri){String host=uri.getHost();return "https".equals(uri.getScheme())&&host!=null&&(host.equals("chatgpt.com")||host.endsWith(".chatgpt.com")||host.equals("openai.com")||host.endsWith(".openai.com"));}
    private String safeAppUrl(String value){
        if(value==null||value.isEmpty())return APP_URL;
        try{Uri uri=Uri.parse(value);String query=uri.getEncodedQuery();return query==null||query.isEmpty()?APP_URL:APP_URL+"/?"+query;}catch(Exception ignored){return APP_URL;}
    }

    private void requestNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}
    private void requestAlarmAccess(){if(Build.VERSION.SDK_INT>=31&&!AlarmScheduler.canSchedule(this))startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}
    private void pickTone(){Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM).putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false).putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,getToneUri());startActivityForResult(i,PICK_TONE);}
    private Uri getToneUri(){String value=getPreferences(MODE_PRIVATE).getString("tone","");return value.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(value);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK_TONE&&result==RESULT_OK&&data!=null){Uri uri=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);if(uri!=null){getPreferences(MODE_PRIVATE).edit().putString("tone",uri.toString()).apply();webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('takt-alarm-tone-changed'))",null);Toast.makeText(this,"Alarmton gespeichert",Toast.LENGTH_SHORT).show();}}}
    private void testAlarm(){if(!AlarmScheduler.canSchedule(this)){requestAlarmAccess();Toast.makeText(this,"Erlaube zuerst Alarme und Erinnerungen, dann tippe erneut auf Test.",Toast.LENGTH_LONG).show();return;}try{JSONObject data=new JSONObject().put("id","test-alarm").put("title","Dein Takt-Alarm funktioniert").put("triggerAt",System.currentTimeMillis()+5000).put("mode","alarm").put("tone",getToneUri().toString()).put("url",APP_URL);AlarmScheduler.schedule(this,data,false);Toast.makeText(this,"Der Alarm startet in 5 Sekunden.",Toast.LENGTH_SHORT).show();}catch(Exception ignored){}}
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}

    public class TaktBridge {
        @JavascriptInterface public String capabilities(){return "{\"nativeAlarm\":true,\"tonePicker\":true,\"version\":1}";}
        @JavascriptInterface public boolean scheduleAlarm(String json){try{JSONObject data=new JSONObject(json);if(data.optString("tone").isEmpty())data.put("tone",getToneUri().toString());if(!AlarmScheduler.canSchedule(MainActivity.this)){runOnUiThread(()->requestAlarmAccess());return false;}return AlarmScheduler.schedule(MainActivity.this,data,true);}catch(Exception e){return false;}}
        @JavascriptInterface public void cancelAlarm(String id){AlarmScheduler.cancel(MainActivity.this,id);}
        @JavascriptInterface public void chooseTone(){runOnUiThread(()->pickTone());}
        @JavascriptInterface public void testAlarm(){runOnUiThread(()->MainActivity.this.testAlarm());}
        @JavascriptInterface public String selectedTone(){return getToneUri().toString();}
        @JavascriptInterface public void requestAlarmPermission(){runOnUiThread(()->requestAlarmAccess());}
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
