package com.example.bsafe.utils;

/** Used to easily send MSD into the database **/
public class Alert {

    private String latitude;
    private String longitude;
    private String timestamp;
    private String speed;
    private String gforce;

    public Alert(String ltd, String lgd, String ts, String s, String g){
        this.latitude = ltd;
        this.longitude = lgd;
        this.timestamp = ts;
        this.speed = s;
        this.gforce = g;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSpeed() {
        return speed;
    }

    public String getGforce() {
        return gforce;
    }


}
