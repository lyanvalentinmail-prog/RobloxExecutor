package com.executor.workspace;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.provider.DocumentsContract;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.io.OutputStream;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.webkit.WebViewAssetLoader;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri folderUri;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder().addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        String storedFolder = getPreferences(MODE_PRIVATE).getString("folder", null);
        if (storedFolder != null) folderUri = Uri.parse(storedFolder);
        web.addJavascriptInterface(new StorageBridge(), "AndroidStorage");
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("appassets.androidplatform.net".equals(uri.getHost()) && "/api/search".equals(uri.getPath())) {
                    try {
                        String query = uri.getQueryParameter("q");
                        URL url = new URL("https://scriptblox.com/api/script/search?q=" + java.net.URLEncoder.encode(query == null ? "" : query, "UTF-8") + "&max=20");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                        connection.setRequestProperty("User-Agent", "ExecutorWorkspace/1.0");
                        if (connection.getResponseCode() != 200) throw new java.io.IOException("HTTP error");
                        return new WebResourceResponse("application/json", "UTF-8", connection.getInputStream());
                    } catch (Exception ex) {
                        return new WebResourceResponse("application/json", "UTF-8", 502, "Bad Gateway", java.util.Collections.emptyMap(), new ByteArrayInputStream("{\"error\":\"No se pudo conectar con ScriptBlox.\"}".getBytes(StandardCharsets.UTF_8)));
                    }
                }
                return loader.shouldInterceptRequest(uri);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return !"appassets.androidplatform.net".equals(request.getUrl().getHost()); }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), 100); return true; }
                catch (Exception e) { fileCallback = null; callback.onReceiveValue(null); return false; }
            }
        });
        setContentView(web);
        web.loadUrl("https://appassets.androidplatform.net/index.html");
    }
    private class StorageBridge {
        @JavascriptInterface public void chooseFolder() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, 101);
            });
        }
        @JavascriptInterface public boolean hasFolder() { return folderUri != null; }
        @JavascriptInterface public void saveScript(String name, String contents) {
            new Thread(() -> {
                try {
                    if (folderUri == null) throw new IllegalStateException();
                    String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".lua";
                    Uri tree = folderUri;
                    Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
                    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
                    Uri file = null;
                    try (android.database.Cursor cursor = getContentResolver().query(children, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                        if (cursor != null) while (cursor.moveToNext()) {
                            if (safeName.equals(cursor.getString(1))) { file = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)); break; }
                        }
                    }
                    if (file == null) file = DocumentsContract.createDocument(getContentResolver(), parent, "text/plain", safeName);
                    if (file == null) throw new java.io.IOException();
                    try (OutputStream output = getContentResolver().openOutputStream(file, "wt")) {
                        if (output == null) throw new java.io.IOException();
                        output.write(contents.getBytes(StandardCharsets.UTF_8));
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Script guardado en la carpeta seleccionada", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo guardar en la carpeta. Vuelve a seleccionarla.", Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selected = data.getData();
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(selected, flags);
                folderUri = selected;
                getPreferences(MODE_PRIVATE).edit().putString("folder", selected.toString()).apply();
                Toast.makeText(this, "Carpeta seleccionada para guardar scripts", Toast.LENGTH_SHORT).show();
            } catch (SecurityException ex) { Toast.makeText(this, "No se pudo acceder a la carpeta", Toast.LENGTH_LONG).show(); }
        }
        if (requestCode == 100 && fileCallback != null) { fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)); fileCallback = null; }
    }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
