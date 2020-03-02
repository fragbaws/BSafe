package com.example.cda.utils;

public class Constant {

    /** Metric data **/
    public static final double MS2KMH = 3.6;
    public static final double SECS2MS = 1000;
    public static final float NS2S = 1.0f / 1000000000.0f;
    public static final double RAD2D = 180.0 / Math.PI;
    public static final double GRAVITY_CONSTANT = 9.81;

    /*Intervals (secs) at which each data is recorded*/
    public static final int BUFFER_SIZE = 10;
    public static final double LOCATION_INTERVAL_SECS = 1;
    public static final double ACCELEROMETER_INTERVAL_SECS = .02;
    public static final double GYROSCOPE_INTERVAL_SECS = .02;
    public static final double MICROPHONE_INTERVAL_SECS = .2;

    /* Alert Dialog */
    public static final int AUTO_DISMISS_MILLIS = 10000;
    public static final int COUNT_DOWN_INTERVAL_MILLIS = 10000;

    /*Google Map*/
    public static final int CLOSE_ZOOM = 15;
    
    /*Thresholds*/
    public static final int ACCIDENT_THRESHOLD = 1;
    public static final int LOW_SPEED_ACCIDENT_THRESHOLD = 3;
    public static final int G_FORCE_THRESHOLD = 4;
    public static final double STANDARD_DEVIATION_THRESHOLD = 2.06;
    public static final int SOUND_PRESSURE_LEVEL_THRESHOLD = 140;
    public static final int ROTATION_THRESHOLD = 45;
    public static final int VEHICLE_SPEED_THRESHOLD = 24;

}
