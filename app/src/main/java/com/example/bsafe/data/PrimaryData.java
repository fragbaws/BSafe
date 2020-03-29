package com.example.bsafe.data;

import com.example.bsafe.utils.Buffer;
import com.example.bsafe.utils.Constants;

public class PrimaryData {

    /** Records for last X seconds period, each entry is running average over Y seconds as each entry
     * is calculated with each location callback which can have a delay of >=1 second **/
    private Buffer<Double> bufferSpeed;

    /** Records for last 5 seconds period, each entry is running average over 1 second **/
    private Buffer<Double> bufferOrientation;

    /** Records for last 10 second period, each entry is a running average over 1 second **/
    private Buffer<Double> bufferGForce;

    public PrimaryData(){
        bufferSpeed = new Buffer<>(Constants.EXTERNAL_DATA_BUFFER_SIZE); // accurate running average
        bufferOrientation = new Buffer<>(Constants.EXTERNAL_DATA_BUFFER_SIZE); // check for change in fixed position
        bufferGForce = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE);
    }

    public Buffer<Double> getBufferGForce() { return this.bufferGForce; }

    public Buffer<Double> getBufferOrientation() { return this.bufferOrientation; }


    public Buffer<Double> getBufferSpeed(){ return this.bufferSpeed; }


    public void clearBuffers(){
        this.bufferSpeed.clear();
        this.bufferGForce.clear();
        this.bufferOrientation.clear();
    }

}
