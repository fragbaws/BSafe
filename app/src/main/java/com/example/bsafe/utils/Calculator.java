package com.example.bsafe.utils;

import android.hardware.SensorManager;
import android.location.Location;

import java.util.List;

/**
 * Singleton class that contains formulas used to perform various required calculations
 */
public class Calculator {

    private static Calculator instance = null;

    private Calculator(){

    }

    public static Calculator getInstance(){
        if (instance == null){
            instance = new Calculator();
        }
        return instance;
    }

    public double calculateCurrentSpeed(Location prev, Location curr){
        float time = (float) ((curr.getTime() - prev.getTime()) / Constants.SECS2MS);
        float distance = curr.distanceTo(prev);
        float speed = distance / time;
        if (speed < 1 || Double.isNaN(speed) || Double.isInfinite(speed)) {
            speed = 0;
        }
        return (speed*Constants.MS2KMH);
    }

    public double calculateGForce(double x, double y, double z){
        return Math.sqrt(x*x + y*y + z*z)/(Constants.GRAVITY_CONSTANT);
    }

    public double calculateOrientation(float[] gravityValues, float[] magneticValues){
        float R[] = new float[9];
        float I[] = new float[9];

        boolean success = SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);
        if(success) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            orientation[1] = (float) Math.toDegrees(orientation[1]); // pitch
            orientation[2] = (float) Math.toDegrees(orientation[2]); // roll
            return orientation[1] + orientation[2];
        }
        return Double.NEGATIVE_INFINITY;
    }

    public double calculateRateOfChange(double prevVal, double currVal, float deltaTime){
        double deltaValues = currVal - prevVal;
        return deltaValues/deltaTime;
    }

    public double calculateAverage(List<Double> list){
        double sum = 0;
        for(double num: list){
            sum+=num;
        }
        return sum/list.size();
    }
}
