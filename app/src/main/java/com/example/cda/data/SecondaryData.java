package com.example.cda.data;

import com.example.cda.utils.CircularQueue;

public class SecondaryData {

    /** Records for last 10 second period, each entry is running average over 1 second **/
    private CircularQueue<Double> bufferSpeed;
    private CircularQueue<Double> bufferGForce;
    private CircularQueue<Double> bufferOmega;
    private CircularQueue<Double> bufferDecibels;

    /*Rate of change*/
    private CircularQueue<Double> bufferAcceleration;
    private CircularQueue<Double> bufferAngularAcceleration;
    private CircularQueue<Double> bufferGForceJerk;
    private CircularQueue<Double> bufferDecibelROC;


    public SecondaryData(int capacity){
        bufferAcceleration = new CircularQueue<>(capacity-1);
        bufferAngularAcceleration = new CircularQueue<>(capacity-1);
        bufferGForceJerk = new CircularQueue<>(capacity-1);
        bufferDecibelROC = new CircularQueue<>(capacity-1);

        bufferSpeed = new CircularQueue<>(capacity);
        bufferGForce = new CircularQueue<>(capacity);
        bufferOmega = new CircularQueue<>(capacity);
        bufferDecibels = new CircularQueue<>(capacity);
    }

    public CircularQueue<Double> getBufferSpeed() {
        return bufferSpeed;
    }

    public CircularQueue<Double> getBufferGForce() {
        return bufferGForce;
    }

    public CircularQueue<Double> getBufferOmega() {
        return bufferOmega;
    }

    public CircularQueue<Double> getBufferDecibels() {
        return bufferDecibels;
    }

    public CircularQueue<Double> getBufferAcceleration() {
        return bufferAcceleration;
    }

    public CircularQueue<Double> getBufferAngularAcceleration() {
        return bufferAngularAcceleration;
    }

    public CircularQueue<Double> getBufferGForceJerk() {
        return bufferGForceJerk;
    }

    public CircularQueue<Double> getBufferDecibelROC() {
        return bufferDecibelROC;
    }

}
