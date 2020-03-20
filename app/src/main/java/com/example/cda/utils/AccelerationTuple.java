package com.example.cda.utils;

/** Used with rate of change values that are not calculated over a consistent fixed period
 * i.e. acceleration
 */
public class AccelerationTuple {

    private double value;
    private float dT;

    public AccelerationTuple(double val, float dt){
        this.value = val;
        this.dT = dt;
    }

    public double getValue() {
        return value;
    }

    public float getdT() {
        return dT;
    }

    @Override
    public String toString(){
        return "("+this.value +" m/s^2, "+ this.dT +" s)";
    }


}
