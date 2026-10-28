package ir.ef3.carcare;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP = 4101;
    private static final int OPEN_BACKUP = 4102;
    private static final int NOTIFICATION_PERMISSION = 4103;
    private WebView webView;
    private String pendingBackup = "{}";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AppBridge(), "AndroidApp");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "car_care_reminders", "یادآوری‌های خودرو", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("یادآوری روزانه بررسی سرویس و کارکرد خودرو");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private class AppBridge {
        @JavascriptInterface public void exportBackup(String json) {
            runOnUiThread(() -> {
                pendingBackup = json == null ? "{}" : json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "khodro-man-backup.json");
                startActivityForResult(intent, CREATE_BACKUP);
            });
        }
        @JavascriptInterface public void importBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, OPEN_BACKUP);
            });
        }
        @JavascriptInterface public void toast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
        @JavascriptInterface public void setReminder(boolean enabled, String time, String details) {
            runOnUiThread(() -> {
                SharedPreferences prefs = getSharedPreferences("car_care", MODE_PRIVATE);
                prefs.edit().putBoolean("reminder_enabled", enabled)
                    .putString("reminder_time", time == null ? "09:00" : time)
                    .putString("reminder_details", details == null ? "" : details).apply();
                if (enabled) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
                    }
                    ReminderReceiver.schedule(MainActivity.this, time);
                    Toast.makeText(MainActivity.this, "یادآوری روزانه فعال شد", Toast.LENGTH_SHORT).show();
                } else {
                    ReminderReceiver.cancel(MainActivity.this);
                    Toast.makeText(MainActivity.this, "یادآوری روزانه غیرفعال شد", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == CREATE_BACKUP) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out != null) out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(this, "فایل پشتیبان ذخیره شد", Toast.LENGTH_LONG).show();
            } else if (requestCode == OPEN_BACKUP) {
                StringBuilder sb = new StringBuilder();
                try (InputStream in = getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
                String encoded = android.util.Base64.encodeToString(
                    sb.toString().getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
                webView.evaluateJavascript("window.receiveBackupFromAndroid('" + encoded + "')", null);
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطا در کار با فایل: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}