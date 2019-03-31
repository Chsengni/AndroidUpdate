package com.ch.androidupdate;

public class UpdateInfo {
    private String version;
    private String APKurl;
    private String desc;
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setAPKurl(String APKurl) {
        this.APKurl = APKurl;
    }

    public String getAPKurl() {
        return APKurl;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
