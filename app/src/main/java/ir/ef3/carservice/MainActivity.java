package ir.ef3.carservice;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.view.View;
import android.view.Window;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE_BACKUP = 7301;
    private static final int REQUEST_OPEN_BACKUP = 7302;
    private WebView webView;
    private String pendingBackupJson = "";
    private String pendingBackupFilename = "car-service-backup.json";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(238,242,248));
        window.setNavigationBarColor(Color.rgb(238,242,248));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new BackupBridge(), "AndroidBackup");
        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else webView.loadUrl("file:///android_asset/index.html");
    }

    private class BackupBridge {
        @JavascriptInterface public void saveBackup(String filename, String json) {
            if(json==null||json.isEmpty())return;
            pendingBackupJson=json;
            pendingBackupFilename=(filename==null||filename.trim().isEmpty())?"car-service-backup.json":filename;
            runOnUiThread(() -> {
                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE,pendingBackupFilename);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try{startActivityForResult(intent,REQUEST_SAVE_BACKUP);}
                catch(Exception e){notifyJsSave(false,"باز کردن پنجره ذخیره فایل ناموفق بود");}
            });
        }
        @JavascriptInterface public void openBackup() {
            runOnUiThread(() -> {
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try{startActivityForResult(intent,REQUEST_OPEN_BACKUP);}
                catch(Exception e){notifyJsImportError("باز کردن انتخاب‌گر فایل ناموفق بود");}
            });
        }
    }

    private void notifyJsSave(boolean success,String message){
        if(webView==null)return;
        String safe=android.util.JSONObject.quote(message==null?"":message);
        webView.post(() -> webView.evaluateJavascript("window.onNativeBackupSaved&&window.onNativeBackupSaved("+success+","+safe+")",null));
    }
    private void notifyJsImportError(String message){
        if(webView==null)return;
        String safe=android.util.JSONObject.quote(message==null?"":message);
        webView.post(() -> webView.evaluateJavascript("window.onNativeBackupSaved&&window.onNativeBackupSaved(false,"+safe+")",null));
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(requestCode==REQUEST_SAVE_BACKUP){
            try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){
                if(out==null)throw new IOException("فایل قابل نوشتن نیست");
                out.write(pendingBackupJson.getBytes("UTF-8"));
                out.flush();
                pendingBackupJson="";
                notifyJsSave(true,"");
            }catch(Exception e){notifyJsSave(false,"ذخیره فایل JSON انجام نشد");}
        }else if(requestCode==REQUEST_OPEN_BACKUP){
            try(InputStream in=getContentResolver().openInputStream(uri);
                ByteArrayOutputStream out=new ByteArrayOutputStream()){
                if(in==null)throw new IOException("فایل قابل خواندن نیست");
                byte[] buffer=new byte[8192];int count;long total=0;
                while((count=in.read(buffer))!=-1){
                    total+=count;
                    if(total>20L*1024L*1024L)throw new IOException("فایل بیش از ۲۰ مگابایت است");
                    out.write(buffer,0,count);
                }
                String json=new String(out.toByteArray(),"UTF-8");
                String quoted=android.util.JSONObject.quote(json);
                webView.post(() -> webView.evaluateJavascript("window.onNativeBackupImported&&window.onNativeBackupImported("+quoted+")",null));
            }catch(Exception e){notifyJsImportError("خواندن فایل JSON ناموفق بود");}
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) { webView.saveState(outState); super.onSaveInstanceState(outState); }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}