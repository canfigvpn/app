package ir.ef3.carcare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP = 4101;
    private static final int OPEN_BACKUP = 4102;
    private WebView webView;
    private String pendingBackup = "{}";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private class AppBridge {
        @JavascriptInterface public void exportBackup(String json) {
            runOnUiThread(() -> {
                pendingBackup = json == null ? "{}" : json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "Peugeot206Care-backup.json");
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
                String encoded = android.util.Base64.encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
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
