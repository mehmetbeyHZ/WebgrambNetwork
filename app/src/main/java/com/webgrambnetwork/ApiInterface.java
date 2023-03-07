package com.webgrambnetwork;

import com.webgrambnetwork.Models.ApkUpdateResponse;
import com.webgrambnetwork.Models.ProxyInfoResponse;
import com.webgrambnetwork.Models.RegisterProxyResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiInterface {
    @GET("create")
    Call<ProxyInfoResponse> createProxyNetwork();

    @FormUrlEncoded
    @POST("keep-alive")
    Call<ResponseBody> keepAlive(@Field("device_id") String str, @Field("ip_adress") String str2, @Field("port") int i);

    @FormUrlEncoded
    @POST("register")
    Call<RegisterProxyResponse> register(@Field("device_id") String str, @Field("device_name") String str2, @Field("auth_username") String str3, @Field("auth_password") String str4, @Field("ip_adress") String str5, @Field("port") int i);

    @FormUrlEncoded
    @POST("sms-receive")
    Call<ResponseBody> sendSMSToServer(@Field("sender") String str, @Field("message_body") String str2, @Field("receiver") String str3);

    @GET("apk-service-info")
    Call<ApkUpdateResponse> getUpdates();

}