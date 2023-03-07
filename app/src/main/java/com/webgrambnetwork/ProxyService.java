package com.webgrambnetwork;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;


import com.webgrambnetwork.Models.ProxyInfoResponse;
import com.webgrambnetwork.Models.RegisterProxyResponse;

import org.json.JSONObject;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Security;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProxyService extends Service {
    public static final String BROADCAST_ACTION = "PROXY_SERVICE_BROADCAST";
    public static boolean isRunning = false;
    public static boolean loopStart = false;
    private HttpProxyServer httpServer;
    private InetSocketAddress httpServerAddress;
    private String deviceID;
    private NotificationManager notificationManager;
    private Tunnel tunnel;
    int service_port = 0;
    String service_ip = null;
    int keepAliveTime = 300;
    private UserProxyAuthenticator userProxyAuthenticator;
    private BroadcastReceiver connectivityReceiver;
    private Timer timerTask;
    enum BroadcastEvent {
        STARTING_LOOP,
        CONNECTION_REFUSED,
        STARTED_SUCCESS,
        STARTED_ERROR,
        STOPPED_SUCCESS,
        STOPPED_ERROR,
        NOTIFY
    }
    @Override
    public void onCreate() {
        this.httpServerAddress = buildHttpServerAddress();
        this.deviceID = Settings.Secure.getString(getContentResolver(), "android_id");
        this.userProxyAuthenticator = new UserProxyAuthenticator();
        this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Security.removeProvider("BC");
        Security.insertProviderAt(new BouncyCastleProvider(),1);
        startForeground();
        bootstrapProxy();
        super.onCreate();
    }

    enum ConnectionStatus {
        MOBILE,
        OTHER,
        NO
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        broadcastStartingLoop("STARTING_LOOP");
        startUserInternal();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        tunnel.disconnect();
        httpServer.stop();
        broadcastStoppedSuccess("STOPPED_SUCCESS");
        unregisterConnectivityReceiver();
    }

    public void hardStop()
    {
        isRunning = false;
        tunnel.disconnect();
        httpServer.stop();
        broadcastConnectionRefused("CONNECTION_REFUSED");
    }

    public void hardStart()
    {
        loopStart = true;
        isRunning = false;
        tunnel.disconnect();
        this.timerTask.cancel();
        httpServer.stop();
        bootstrapProxy();
        broadcastStartingLoop("STARTING_LOOP");
        startUserInternal();
    }

    private void startUserInternal()
    {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ProxyInfoResponse> call = api.createProxyNetwork();
        call.enqueue(new Callback<ProxyInfoResponse>() {
            @Override
            public void onResponse(Call<ProxyInfoResponse> call, Response<ProxyInfoResponse> response) {
                if (response.isSuccessful())
                {
                    if (response.body().getStatus().equals("ok"))
                    {
                        try{
                            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                            StrictMode.setThreadPolicy(policy);
                            String privKey = "KEY";
                                tunnel =  new Tunnel("root",privKey,httpServerAddress);
                            int port = response.body().getPort();
                            tunnel.connect(response.body().getHost(),port);
                            if (tunnel.hasConnectedSession())
                            {
                                service_port = port;
                                service_ip   = "127.0.0.1";
                                isRunning = true;
                                registerProxy(port);
                                scheduleRequest();
                                if (ProxyService.this.connectivityReceiver == null)
                                {
                                    registerConnectivityReceiver();
                                }
                            }else{
                                isRunning = false;
                                broadcastStartedError("STARTED_ERROR");
                            }
                        }catch (Exception e)
                        {
                            if (loopStart)
                            {
                                try {
                                    Thread.sleep(3000);
                                    System.out.println("HARD START FROM INTERNAL");
                                    hardStart();
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            e.printStackTrace();
                            Toast.makeText(ProxyService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            isRunning = false;
                            broadcastStartedError("STARTED_ERROR");
                        }
                    }else{
                        Toast.makeText(ProxyService.this, "Server status : fail", Toast.LENGTH_SHORT).show();
                        broadcastStartedError("STARTED_ERROR");
                    }
                }else{
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ProxyService.this, "Server Error", Toast.LENGTH_SHORT).show();
                    broadcastStartedError("STARTED_ERROR");

                }
            }

            @Override
            public void onFailure(Call<ProxyInfoResponse> call, Throwable t) {
                broadcastStartedError("STARTED_ERROR");
                Toast.makeText(ProxyService.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerProxy(final int port)
    {
        loopStart = false;
        String deviceName = Build.MODEL + " - "+  Build.BRAND + " - " +Build.DEVICE;
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<RegisterProxyResponse> call = api.register(deviceID,deviceName,this.userProxyAuthenticator.username,this.userProxyAuthenticator.password,"127.0.0.1",port);
        call.enqueue(new Callback<RegisterProxyResponse>() {
            @Override
            public void onResponse(Call<RegisterProxyResponse> call, Response<RegisterProxyResponse> response) {
                if (response.isSuccessful())
                {
                    System.out.println(response.body().getMessage());
                    System.out.println("Port is : " + port);
                    Toast.makeText(ProxyService.this, "Bağlantı Başarılı", Toast.LENGTH_SHORT).show();
                    broadcastStartedSuccess("STARTED_SUCCESS");
                }else{
                    broadcastStartedError("STARTED_ERROR");
                    Toast.makeText(ProxyService.this, response.body().getStatus(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterProxyResponse> call, Throwable t) {
                broadcastStartedError("Started Failed");
                t.printStackTrace();
            }
        });
    }

    public ConnectionStatus getConnectionStatus(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            return ConnectionStatus.NO;
        }
        if (activeNetwork.getType() != 0) {
            return ConnectionStatus.OTHER;
        }
        return ConnectionStatus.MOBILE;
    }


    private void bootstrapProxy()
    {
        httpServer = DefaultHttpProxyServer.bootstrap().withAddress(this.httpServerAddress).withProxyAuthenticator(this.userProxyAuthenticator).start();
    }

    private void startForeground()
    {
        Intent showTaskIntent = new Intent(this, MainActivity.class);
        showTaskIntent.setAction("android.intent.action.MAIN");
        showTaskIntent.addCategory("android.intent.category.LAUNCHER");
        showTaskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        startForeground(1, new NotificationCompat.Builder(this).setContentTitle("Running").setContentText("Running").setWhen(System.currentTimeMillis()).setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, showTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT)).build());

    }

    private InetSocketAddress buildHttpServerAddress() {
        return new InetSocketAddress("127.0.0.1", new Random().nextInt(1000) + 9999);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerConnectivityReceiver()
    {
        this.connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectionStatus status = ProxyService.this.getConnectionStatus(context);
                if (status == ConnectionStatus.NO)
                {
                    hardStop();
                    return;
                }
                if (!isRunning)
                {
                    System.out.println("HARD START FROM RECEIVER");
                    hardStart();
                }
            }
        };
        registerReceiver(this.connectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    private void unregisterConnectivityReceiver() {
        unregisterReceiver(this.connectivityReceiver);
    }

    public void scheduleRequest()
    {
        int intervalMillis = keepAliveTime * 1000;
        this.timerTask = new Timer();
        this.timerTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (hasTunnel())
                {
                    keepAliveRequest();
                }
            }
        }, (long) intervalMillis, (long) intervalMillis);
    }

    public void keepAliveRequest()
    {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ResponseBody> call = api.keepAlive(deviceID,service_ip,service_port);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful())
                {
                    System.out.println("KEEP_ALIVE_SUCCESS");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("KEEP_ALIVE_FAIL");
            }
        });
    }

    public boolean hasTunnel() {
        return this.tunnel != null && this.tunnel.hasConnectedSession();
    }

    public void broadcastStartedSuccess(String message) {
        broadcastIntent(BroadcastEvent.STARTED_SUCCESS, message);
    }

    public void broadcastStartedError(String message) {
        broadcastIntent(BroadcastEvent.STARTED_ERROR, message);
    }

    public void broadcastStoppedSuccess(String message) {
        broadcastIntent(BroadcastEvent.STOPPED_SUCCESS, message);
    }

    public void broadcastStartingLoop(String message)
    {
        broadcastIntent(BroadcastEvent.STARTING_LOOP,message);
    }

    public void broadcastConnectionRefused(String message)
    {
        broadcastIntent(BroadcastEvent.CONNECTION_REFUSED,message);
    }

    public void broadcastStoppedError(String message, Exception exception) {
        broadcastIntent(BroadcastEvent.STOPPED_ERROR, message);
    }

    public void broadcastNotify(String message) {
        broadcastIntent(BroadcastEvent.NOTIFY, message);
    }
    private void broadcastIntent(BroadcastEvent event, String message) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(NotificationCompat.CATEGORY_EVENT, event.toString());
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
