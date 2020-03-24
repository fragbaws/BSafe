package com.example.cda.utils;

import android.location.Location;
import android.media.MediaRecorder;

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
        float time = (float) ((curr.getTime() - prev.getTime()) / Constants.SECS2MS);
        float distance = curr.distanceTo(prev);
        float speed = distance / time;
        if (speed < 1 || Double.isNaN(speed) || Double.isInfinite(speed)) {
            speed = 0;
        }
        return (speed*Constants.MS2KMH);
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

    public double calculateRotation(float x, float y, float z, float prevTime, float currTime){
        float pitch = x;
        float roll = y;
        float yaw = z;
        double omegaMag = Math.sqrt(pitch*pitch + roll*roll); // d0/dt
        double dt = (currTime - prevTime)/Constants.SECS2MS;

        double rotation = omegaMag * dt * Constants.RAD2D;

        return rotation;
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
