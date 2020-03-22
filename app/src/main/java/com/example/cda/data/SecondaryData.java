package com.example.cda.data;

import com.example.cda.utils.AccelerationTuple;
import com.example.cda.utils.Buffer;
import com.example.cda.utils.Constants;

public class SecondaryData {

    /** Records for last X seconds period, each entry is running average over Y seconds as each entry
     * is calculated with each location callback which can have a delay of >=1 second **/
    private Buffer<AccelerationTuple> bufferAcceleration;

    /** Records for last 10 second period, each entry is a running average over 1 second **/
    private Buffer<Double> bufferAngularAcceleration;
    private Buffer<Double> bufferGForceJerk;
    private Buffer<Double> bufferDecibelROC;


    public SecondaryData(){
        bufferAcceleration = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
        bufferAngularAcceleration = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
        bufferGForceJerk = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
        bufferDecibelROC = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
    }

    public Buffer<AccelerationTuple> getBufferAcceleration() {
        return bufferAcceleration;
    }

    public Buffer<Double> getBufferAngularAcceleration() {
        return bufferAngularAcceleration;
    }

    public Buffer<Double> getBufferGForceJerk() {
        return bufferGForceJerk;
    }

    public Buffer<Double> getBufferDecibelROC() {
        return bufferDecibelROC;
    }

    public void clearBuffers(){
        this.bufferAcceleration.clear();
        this.bufferAngularAcceleration.clear();
        this.bufferDecibelROC.clear();
        this.bufferGForceJerk.clear();
    }

}
