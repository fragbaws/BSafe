package com.example.cda.data;

import com.example.cda.utils.CircularQueue;

public class SecondaryData {

    /** Records for last 10 second period, each entry is running average over 1 second **/
    private CircularQueue<Double> bufferSpeed;
    private CircularQueue<Double> bufferGForce;
    private CircularQueue<Double> bufferRotation;
    private CircularQueue<Double> bufferDecibels;

    /*Rate of change*/
    private CircularQueue<Double> bufferSpeedROC;
    private CircularQueue<Double> bufferRotationROC;
    private CircularQueue<Double> bufferGForceROC;
    private CircularQueue<Double> bufferDecibelROC;


    public SecondaryData(int capacity){
        bufferSpeedROC = new CircularQueue<>(capacity-1);
        bufferRotationROC = new CircularQueue<>(capacity-1);
        bufferGForceROC = new CircularQueue<>(capacity-1);
        bufferDecibelROC = new CircularQueue<>(capacity-1);


        bufferSpeed = new CircularQueue<>(capacity);
        bufferGForce = new CircularQueue<>(capacity);
        bufferRotation = new CircularQueue<>(capacity);
        bufferDecibels = new CircularQueue<>(capacity);
    }

    public CircularQueue<Double> getBufferSpeed() {
        return bufferSpeed;
    }

    public CircularQueue<Double> getBufferGForce() {
        return bufferGForce;
    }

    public CircularQueue<Double> getBufferRotation() {
        return bufferRotation;
    }

    public CircularQueue<Double> getBufferDecibels() {
        return bufferDecibels;
    }

    public CircularQueue<Double> getBufferSpeedROC() {
        return bufferSpeedROC;
    }

    public CircularQueue<Double> getBufferRotationROC() {
        return bufferRotationROC;
    }

    public CircularQueue<Double> getBufferGForceROC() {
        return bufferGForceROC;
    }

    public CircularQueue<Double> getBufferDecibelROC() {
        return bufferDecibelROC;
    }


}
