package com.example.bsafe.data;

import com.example.bsafe.utils.AccelerationTuple;
import com.example.bsafe.utils.Buffer;
import com.example.bsafe.utils.Constants;

public class SecondaryData {

    /** Records for last X seconds period, each entry is running average over Y seconds as each entry
     * is calculated with each location callback which can have a delay of >=1 second **/
    private Buffer<AccelerationTuple> bufferAcceleration;

    public SecondaryData(){
        bufferAcceleration = new Buffer<>(Constants.INTERNAL_DATA_BUFFER_SIZE-1);
    }

    public Buffer<AccelerationTuple> getBufferAcceleration() {
        return bufferAcceleration;
    }

    public void clearBuffers(){
        this.bufferAcceleration.clear();
    }

}
