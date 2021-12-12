package com.example.ee595android;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.net.URL;

import com.example.ee595android.YoutubeService.LocalBinder;

import com.example.ee595android.ServiceFragment.ServiceFragmentDelegate;


public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private WebSettings mWebSettings;
    Intent mIntent;
    URL url;
    boolean mBounded;
    YoutubeService mService;

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBounded = true;
            LocalBinder mLocalBinder = (LocalBinder) iBinder;
            mService = mLocalBinder.getServerInstance();
            Log.d("BIND","BIND");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBounded = false;
            mService = null;
            Log.d("BIND", "UNBIND");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String message = intent.getStringExtra("Link");

        if(message == null){
            Log.d("EE595B", "started by hand");
            mIntent = new Intent(
                    getApplicationContext(),
                    YoutubeService.class
            );

            // startService(service);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        }
        else{
            message = message.replace("watch?v=", "v/");

            mWebView = findViewById(R.id.wvLayout);
            mWebView.loadUrl(message);

            mWebView.addJavascriptInterface(new HtmlInterface(this), "HtmlViewer");
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void doUpdateVisitedHistory(WebView view, String url, boolean isReload){
                    super.doUpdateVisitedHistory(view, url, isReload);
                    Log.d("EE595_HTML", url);
                }
                @Override
                public void onLoadResource(WebView view, String url){
                    super.onLoadResource(view, url);
                    view.loadUrl("javascript:window.HtmlViewer.setTime" +
                            "(String(parseInt(document.getElementsByClassName('video-stream html5-main-video')[0].currentTime)));");
                    view.loadUrl("javascript:window.HtmlViewer.setURL" +
                            "(document.URL);");
                    Log.d("PLAY", HTMLvalue.getPlay()+"");
                    if(!HTMLvalue.getPlay()){
                        view.loadUrl("javascript:window.HtmlViewer.stopPlay" +
                                "(document.getElementsByClassName('video-stream html5-main-video')[0].pause());");
                        Log.d("Play_STOPPED", HTMLvalue.getPlay()+"");
                        HTMLvalue.setPlay(true);
                    }
                }
                @Override
                public void onPageFinished(WebView view, String url){
                    view.loadUrl("javascript:window.HtmlViewer.setTime" +
                            "(String(parseInt(document.getElementsByClassName('video-stream html5-main-video')[0].currentTime)));");
                    view.loadUrl("javascript:window.HtmlViewer.setURL" +
                            "(document.URL);");
                }
            });

            mWebSettings = mWebView.getSettings();
            mWebSettings.setJavaScriptEnabled(true);
            mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            mWebSettings.setLoadWithOverviewMode(true);
            mWebSettings.setUseWideViewPort(true);
            mWebSettings.setSupportZoom(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stopService(service);
    }

    public class HtmlInterface {
        private Context ctx;

        HtmlInterface(Context ctx){
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void setTime(String time){
            HTMLvalue.setTime(time);
        }

        @JavascriptInterface
        public void setURL(String URL){
            HTMLvalue.setURL(URL);
        }

        @JavascriptInterface
        public void stopPlay(String URL){
            Log.d("HTML","STOP");
        }

    }

}