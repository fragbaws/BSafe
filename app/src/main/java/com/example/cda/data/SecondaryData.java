package com.example.cda.data;

import com.example.cda.utils.AccelerationTuple;
import com.example.cda.utils.CircularQueue;
import com.example.cda.utils.Constants;

public class SecondaryData {

    /** Records for last X seconds period, each entry is running average over Y seconds as each entry
     * is calculated with each location callback which can have a delay of >=1 second **/
    private CircularQueue<Double> bufferSpeed;

    /** Records for last 10 second period, each entry is running average over 1 second **/
    private CircularQueue<Double> bufferGForce;
    private CircularQueue<Double> bufferOmega;
    private CircularQueue<Double> bufferDecibels;

    /** Rate of change of primary data buffers **/
    private CircularQueue<AccelerationTuple> bufferAcceleration;
    private CircularQueue<Double> bufferAngularAcceleration;
    private CircularQueue<Double> bufferGForceJerk;
    private CircularQueue<Double> bufferDecibelROC;


    public SecondaryData(){
        bufferAcceleration = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
        bufferAngularAcceleration = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
        bufferGForceJerk = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
        bufferDecibelROC = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);

        bufferSpeed = new CircularQueue<>(Constants.EXTERNAL_DATA_BUFFER_SIZE);
        bufferGForce = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE);
        bufferOmega = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE);
        bufferDecibels = new CircularQueue<>(Constants.INTERNAL_DATA_BUFFER_SIZE);
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

    public CircularQueue<AccelerationTuple> getBufferAcceleration() {
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
