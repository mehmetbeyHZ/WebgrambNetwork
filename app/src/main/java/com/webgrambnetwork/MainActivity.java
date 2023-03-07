package com.webgrambnetwork;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.webgrambnetwork.Services.MyAccessibilityService;
import com.webgrambnetwork.Services.SmsReceiver;

import org.apache.commons.lang3.StringUtils;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    private static final int PERMISSION_REQUEST_CODE = 100;
    PowerManager powerManager;
    String packageName;
    Button button;

    private final TrustManager[] trustAllCerts= new TrustManager[] {new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }
    } };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setSubtitle("Version " + Config.VERSION_NAME);
        if (ContextCompat.checkSelfPermission(this, "android.permission.RECEIVE_SMS") != 0) {
            ActivityCompat.requestPermissions(this, permissions(), 1);
        }



        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getPackageManager();
        //get a list of installed apps.
        packages = pm.getInstalledApplications(0);



        startSMSReceiver();

        button = findViewById(R.id.startService);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        button.setBackgroundTintList(getResources().getColorStateList(R.color.colorRed));

        powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        packageName = getPackageName();
        stats();
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(NotificationCompat.CATEGORY_EVENT);
                switch (event) {
                    case "STARTED_SUCCESS":
                        button.setText("Servisi Durdur");
                        button.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                        button.setEnabled(true);
                        break;
                    case "STOPPED_SUCCESS":
                        button.setText("Servisi Başlat");
                        button.setBackgroundTintList(getResources().getColorStateList(R.color.colorRed));
                        button.setEnabled(true);
                        break;
                    case "STARTED_ERROR":
                        button.setText("Servisi Başlat");
                        button.setBackgroundTintList(getResources().getColorStateList(R.color.colorRed));
                        button.setEnabled(true);

                        try {
                            Thread.sleep(1000);
                            if (isAccessibilityServiceEnabled(MainActivity.this,MyAccessibilityService.class)){
                                MyAccessibilityService.instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
                            }else{
                                goAcSet();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                    case "CONNECTION_REFUSED":
                        button.setText("INTERNET BAĞLANTISI YOK!");
                        button.setEnabled(false);
                        break;
                    case "STARTING_LOOP":
                        button.setText("BAĞLANTI KURULUYOR...");
                        button.setEnabled(false);
                        break;

                }
                String message = intent.getStringExtra("message");
                renderMessage(message);
            }
        }, new IntentFilter(ProxyService.BROADCAST_ACTION));


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(ProxyService.isRunning);
                if (ProxyService.isRunning) {
                    stopServiceHandler();
                } else {
                    startServiceHandler();
                }
            }
        });
        requestIgnoringBatteryOptimizations();



        final Socket socket;
        try {
            sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
            String phoneNumber = sharedPreferences.getString("phoneNumber", null);
            String deviceID = Settings.Secure.getString(getContentResolver(), "android_id");
            String deviceName = Build.MODEL + " - "+  Build.BRAND + " - " +Build.DEVICE;


            HostnameVerifier myHostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            TrustManager[] trustAllCerts= new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};

            SSLContext mySSLContext = null;
            try {
                mySSLContext = SSLContext.getInstance("TLS");
                try {
                    mySSLContext.init(null, trustAllCerts, null);
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            OkHttpClient okHttpClient = new OkHttpClient.Builder().hostnameVerifier(myHostnameVerifier).sslSocketFactory(mySSLContext.getSocketFactory()).build();

            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);



            IO.Options opts = new IO.Options();
            opts.query = "phone=" + phoneNumber + "&deviceID=" + deviceID+"&deviceName="+deviceName;
            opts.secure = true;
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;


            socket = IO.socket("https://ianaliz.com:9286/", opts);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Args " + args.length);
                System.out.println(args[0].toString());
            }
        });

        socket.on("authenticated", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println(args[0].toString());
            }
        });

        socket.on("RESET_PROXY", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("RESET PROXY REQUEST");
                if (isAccessibilityServiceEnabled(MainActivity.this,MyAccessibilityService.class)){
                    MyAccessibilityService.instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
                }else{
                    goAcSet();
                }
            }
        });

        socket.on("STOP_PROXY", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if(!ProxyService.isRunning)
                {
                    System.out.println("Already Not Running");
                    return;
                }
                stopServiceHandler();
            }
        });

        socket.on("START_PROXY", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (ProxyService.isRunning)
                {
                    System.out.println("Service Already Started");
                    return;
                }
                startServiceHandler();

            }
        });

        socket.connect();



    }

    public void amKillProcess(String process)
    {
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();

        for(ActivityManager.RunningAppProcessInfo runningProcess : runningProcesses)
        {
            if(runningProcess.processName.equals(process))
            {
                android.os.Process.sendSignal(runningProcess.pid, android.os.Process.SIGNAL_KILL);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        stats();
        if (ProxyService.isRunning) {
            disableButton();
        } else {
            activeButton();
        }
    }



    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }

    public void goAcSet()
    {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void disableButton() {
        button.setText("Servisi Durdur");
    }

    private void activeButton() {
        button.setText("Servisi Başlat");
    }

    private void renderMessage(String message) {
        System.out.println(message);
    }

    private void startServiceHandler() {
        if (ContextCompat.checkSelfPermission(this, "android.permission.RECEIVE_SMS") != 0) {
            ActivityCompat.requestPermissions(this, permissions(), 1);
            return;
        }
        Intent intent = new Intent(MainActivity.this, ProxyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                requestIgnoringBatteryOptimizations();
            } else {
                startService(intent);
            }
        } else {
            startService(intent);
        }
    }

    private void stopServiceHandler() {
        stopService(new Intent(this, ProxyService.class));
    }

    private void requestIgnoringBatteryOptimizations() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i = new Intent();
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + packageName));
                startActivity(i);
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @SuppressLint("SetTextI18n")
    private void stats() {
        TextView downloadView = findViewById(R.id.downloadBytes);
        TextView uploadView = findViewById(R.id.uploadBytes);
        TextView totalView = findViewById(R.id.totalBytes);

        String rxHuman;
        String txHuman;
        String totalHuman;
        totalHuman = "0";
        int uid = Process.myUid();
        long rxBytes = TrafficStats.getUidRxBytes(uid);
        long txBytes = TrafficStats.getUidTxBytes(uid);
        long total = rxBytes + txBytes;
        if (rxBytes == -1) {
            rxHuman = "UnSupport";
        } else {
            rxHuman = Formatter.formatFileSize(this, rxBytes);
        }
        if (rxBytes == -1) {
            txHuman = "UnSupport";
        } else {
            txHuman = Formatter.formatFileSize(this, txBytes);
        }

        if (rxBytes != -1 && txBytes != -1) {
            totalHuman = Formatter.formatFileSize(this, total);
        }


        downloadView.setText("Download:" + StringUtils.SPACE + rxHuman);
        uploadView.setText("Upload:" + StringUtils.SPACE + txHuman);
        totalView.setText("Total:" + StringUtils.SPACE + totalHuman);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public String[] permissions() {
        return new String[]{"android.permission.RECEIVE_SMS"};
    }

    private void startSMSReceiver() {
        startService(new Intent(this, SmsReceiver.class));
    }


}
