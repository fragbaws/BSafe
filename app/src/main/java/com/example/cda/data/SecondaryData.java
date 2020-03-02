package com.example.cda.data;

import com.example.cda.utils.CircularQueue;

public class SecondaryData {

    /** Records for last 10 second period, each entry is running average over 1 second **/
    private CircularQueue<Integer> bufferSpeed;
    private CircularQueue<Double> bufferGForce;
    private CircularQueue<Double> bufferRotation;
    private CircularQueue<Double> bufferDecibels;

    /*Rate of change*/
    private CircularQueue<Double> bufferSpeedROC;

    public SecondaryData(int capacity){
        bufferSpeedROC = new CircularQueue<>(capacity-1);

        bufferSpeed = new CircularQueue<>(capacity);
        bufferGForce = new CircularQueue<>(capacity);
        bufferRotation = new CircularQueue<>(capacity);
        bufferDecibels = new CircularQueue<>(capacity);
    }

    public CircularQueue<Integer> getBufferSpeed() {
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


}
