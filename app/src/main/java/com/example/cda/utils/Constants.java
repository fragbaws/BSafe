package com.example.cda.utils;

public class Constants {

    /** Metric data **/
    public static final double MS2KMH = 3.6;
    public static final double SECS2MS = 1000;
    public static final double GRAVITY_CONSTANT = 9.81;

    /*Intervals (secs) at which each data is recorded*/
    public static final int INTERNAL_DATA_BUFFER_SIZE = 10;
    public static final int EXTERNAL_DATA_BUFFER_SIZE = 5;
    public static final double LOCATION_INTERVAL_SECS = 1;

    /* Alert Dialog */
    public static final int AUTO_DISMISS_MILLIS = 10000;
    public static final int COUNT_DOWN_INTERVAL_MILLIS = 1000;

    /*Google Map*/
    public static final int CLOSE_ZOOM = 15;

    
    /*Thresholds*/
    public static final int G_FORCE_THRESHOLD = 4;
    public static final int ROTATION_THRESHOLD = 20;
    public static final int VEHICLE_FIRST_GEAR_SPEED_THRESHOLD = 24;
    public static final double VEHICLE_EMERGENCY_DECELERATION_THRESHOLD = -7.5;
/*
    public static final double VEHICLE_EMERGENCY_DECELERATION_THRESHOLD = -3.0;
    public static final double G_FORCE_THRESHOLD = 1.0;*/

    /*General*/
    public static final int WIGGLE = 5;

}
