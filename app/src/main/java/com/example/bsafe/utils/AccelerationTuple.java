package com.example.bsafe.utils;

/** Used to keep track of the delay between each location for more accurate acceleration calculation**/
public class AccelerationTuple {

    private double value;
    private float time;

    public AccelerationTuple(double val, float time){
        this.value = val;
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public float getTime() {
        return time;
    }

    @Override
    public String toString(){
        return "("+this.value +" m/s^2, "+ this.time +" s)";
    }


}
