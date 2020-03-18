package com.example.cda.data;

import com.example.cda.utils.CircularQueue;

public class PrimaryData {

    private SecondaryData secondaryData;
    private double currentSpeed;
    private double currentGForce;
    private double currentOmega;
    private double currentDecibels;

    public PrimaryData(int capacity){
        secondaryData = new SecondaryData(capacity);
    }

    public double getCurrentSpeed() { return currentSpeed; }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
        secondaryData.getBufferSpeed().add(currentSpeed);
    }

    public double getCurrentGForce() { return currentGForce; }

    public void setCurrentGForce(double currentGForce) { this.currentGForce = currentGForce; }

    public double getCurrentOmega() {
        return currentOmega;
    }

    public void setCurrentOmega(double currentOmega) { this.currentOmega = currentOmega; }

    public double getCurrentDecibels() {
        return currentDecibels;
    }

    public void setCurrentDecibels(double currentDecibels) { this.currentDecibels = currentDecibels; }

    public CircularQueue<Double> getBufferGForce() {
        return secondaryData.getBufferGForce();
    }

    public CircularQueue<Double> getBufferOmega() {
        return secondaryData.getBufferOmega();
    }

    public CircularQueue<Double> getBufferDecibels() {
        return secondaryData.getBufferDecibels();
    }

    public CircularQueue<Double> getBufferSpeed(){
        return secondaryData.getBufferSpeed();
    }

    public CircularQueue<Double> getBufferGForceJerk() {
        return secondaryData.getBufferGForceJerk();
    }

    public CircularQueue<Double> getBufferAngularAcceleration() {
        return secondaryData.getBufferAngularAcceleration();
    }

    public CircularQueue<Double> getBufferDecibelROC() {
        return secondaryData.getBufferDecibelROC();
    }

    public CircularQueue<Double> getBufferAcceleration(){
        return secondaryData.getBufferAcceleration();
    }

    public void clearBuffers(){
        getBufferAcceleration().clear();
        getBufferSpeed().clear();
        getBufferDecibelROC().clear();
        getBufferGForceJerk().clear();
        getBufferGForce().clear();
        getBufferDecibels().clear();
        getBufferOmega().clear();
        getBufferAngularAcceleration().clear();
    }

}
