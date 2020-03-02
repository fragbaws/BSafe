package com.example.cda.data;

import com.example.cda.utils.CircularQueue;

public class PrimaryData {

    private SecondaryData secondaryData;
    private double currentSpeed;
    private double currentGForce;
    private double currentRotation;
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

    public double getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(double currentRotation) { this.currentRotation = currentRotation; }

    public double getCurrentDecibels() {
        return currentDecibels;
    }

    public void setCurrentDecibels(double currentDecibels) { this.currentDecibels = currentDecibels; }

    public CircularQueue<Double> getBufferGForce() {
        return secondaryData.getBufferGForce();
    }

    public CircularQueue<Double> getBufferRotation() {
        return secondaryData.getBufferRotation();
    }

    public CircularQueue<Double> getBufferDecibels() {
        return secondaryData.getBufferDecibels();
    }

    public CircularQueue<Double> getBufferSpeed(){
        return secondaryData.getBufferSpeed();
    }

    public CircularQueue<Double> getBufferGForceROC() {
        return secondaryData.getBufferGForceROC();
    }

    public CircularQueue<Double> getBufferRotationROC() {
        return secondaryData.getBufferRotationROC();
    }

    public CircularQueue<Double> getBufferDecibelROC() {
        return secondaryData.getBufferDecibelROC();
    }

    public CircularQueue<Double> getBufferSpeedROC(){
        return secondaryData.getBufferSpeedROC();
    }

    public void clearBuffers(){
        getBufferSpeedROC().clear();
        getBufferSpeed().clear();
        getBufferDecibelROC().clear();
        getBufferGForceROC().clear();
        getBufferGForce().clear();
        getBufferDecibels().clear();
        getBufferRotation().clear();
        getBufferRotationROC().clear();
    }

}
