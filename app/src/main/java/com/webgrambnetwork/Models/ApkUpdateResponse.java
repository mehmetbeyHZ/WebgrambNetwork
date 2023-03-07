package com.webgrambnetwork.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApkUpdateResponse {

    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("version_code")
    @Expose
    private Double versionCode;
    @SerializedName("apk_url")
    @Expose
    private String apkUrl;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Double versionCode) {
        this.versionCode = versionCode;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public void setApkUrl(String apkUrl) {
        this.apkUrl = apkUrl;
    }

}
