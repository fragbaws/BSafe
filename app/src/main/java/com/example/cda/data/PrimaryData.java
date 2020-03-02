package com.example.cda.data;

import com.example.cda.utils.CircularQueue;

public class PrimaryData {

    private SecondaryData secondaryData;
    private int currentSpeed;
    private double currentGForce;
    private double currentRotation;
    private double currentDecibels;

    public PrimaryData(int capacity){
        secondaryData = new SecondaryData(capacity);
    }

    public int getCurrentSpeed() { return currentSpeed; }

    public void setCurrentSpeed(int currentSpeed) {
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

    public CircularQueue<Integer> getBufferSpeed(){
        return secondaryData.getBufferSpeed();
    }

}
