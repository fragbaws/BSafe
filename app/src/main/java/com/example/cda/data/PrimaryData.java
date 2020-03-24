package com.example.cda.data;

import com.example.cda.utils.Buffer;
import com.example.cda.utils.Constants;

public class PrimaryData {

    /** Records for last X seconds period, each entry is running average over Y seconds as each entry
     * is calculated with each location callback which can have a delay of >=1 second **/
    private Buffer<Double> bufferSpeed;

    /** Records for last 10 second period, each entry is a running average over 1 second **/
    private Buffer<Double> bufferGForce;
    private Buffer<Double> bufferRotation;
    private Buffer<Double> bufferDecibels;

    public PrimaryData(){
        bufferSpeed = new Buffer<>(Constants.EXTERNAL_DATA_BUFFER_SIZE);
        bufferGForce = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE);
        bufferDecibels = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE);
        bufferRotation = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE);

    }

    public Buffer<Double> getBufferGForce() { return this.bufferGForce; }

    public Buffer<Double> getBufferRotation() { return this.bufferRotation; }

    public Buffer<Double> getBufferDecibels() { return this.bufferDecibels; }

    public Buffer<Double> getBufferSpeed(){ return this.bufferSpeed; }


    public void clearBuffers(){
        this.bufferSpeed.clear();
        this.bufferGForce.clear();
        this.bufferDecibels.clear();
        this.bufferRotation.clear();
    }

}
