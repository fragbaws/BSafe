package com.example.cda.utils;

import android.location.Location;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
        float time = (curr.getTime() - prev.getTime()) / 1000;
        float distance = curr.distanceTo(prev);
        float speed = distance / time;
        if (speed < 1 || Double.isNaN(speed) || Double.isInfinite(speed)) {
            speed = 0;
        }
        return (speed*Constants.MS2KMH);
    }

    public double calculateSpeedDeviation(ArrayList<Integer> speeds){
        double tmp = 0;
        for(Integer curr: speeds){
            tmp+=curr;
        }
        double mean = tmp/speeds.size();
        double sum = 0;
        for(Integer curr: speeds){
            sum += Math.pow((curr - mean), 2);
        }
        return Math.sqrt(sum/speeds.size());
    }

    public double calculateDecibels(MediaRecorder recorder){
        if(recorder != null){
            float maxAmplitude = recorder.getMaxAmplitude();
            float db = (float) (20 * Math.log10(maxAmplitude));// / 32767.0f));
            if(db < 0 || Double.isInfinite(db) || Double.isNaN(db)){
                return 0.0;
            }else {
                return db;
            }
        }
        else{
            return 0.0;
        }
    }

    public double calculateGForce(double x, double y, double z){
        return Math.sqrt(x*x + y*y + z*z)/(Constants.GRAVITY_CONSTANT);

    }

    public double calculateResultingRotation(float x, float y, float z, float prevTime, float currTime){
        float dT = (currTime - prevTime) * Constants.NS2S;
        float pitch = x;
        float roll = y;
        float yaw = z;
        float omegaMag = (float) Math.sqrt(pitch*pitch + roll*roll); // d0/dt

        // angular rotation in radians
        float theta = omegaMag * dT;
        return theta * Constants.RAD2D;
    }

    public double calculateRateOfChange(double prevVal, double currVal){
        double dv = currVal - prevVal;
        return dv/1;
    }

    public double calculateAverage(List<Double> list){
        double sum = 0;
        for(double num: list){
            sum+=num;
        }
        return sum/list.size();
    }
}
