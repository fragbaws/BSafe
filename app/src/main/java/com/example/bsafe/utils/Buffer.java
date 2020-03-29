package com.example.bsafe.utils;

import java.util.ArrayDeque;
import java.util.Iterator;

/** FIFO ArrayDeque where elements are added to the front and removed from rear once it is full **/
public class Buffer<E> extends ArrayDeque<E> {

    private int size;

    public Buffer(int capacity) {
        super(capacity);
        this.size = capacity;
    }

    @Override
    public boolean add(E e) {
        if(super.size() == this.size){
            this.removeLast();
        }
        super.addFirst(e);
        return true;
    }

    /**
     * @return first and second element in the Buffer
     */
    public double[] getRecentPair(){
        if(super.size() < 2){
            return null;
        }
        return new double[]{(double) this.toArray()[0], (double) this.toArray()[1]};
    }

    /**
     * @return most recent element in the Buffer
     */
    public E recent(){
        if(super.size() == 0){
            return null;
        }

        return super.getFirst();
    }

    /**
     * Method used to find object containing value e.
     * Used primarily with AccelerationTuple.
     * @param e - the value to be found in the Buffer
     * @return the object holding value e
     */
    public E retrieveObjectWithValue(E e){
        if(super.size() == 0){
            return null;
        }
        if(e instanceof AccelerationTuple) {
            Iterator it = this.iterator();
            int dtTotal = 0; // required to find how long deceleration occurred
            while (it.hasNext()) {
                AccelerationTuple curr = (AccelerationTuple) it.next();
                if (curr.getValue() == ((AccelerationTuple) e).getValue()) {
                    return (E) new AccelerationTuple(curr.getValue(), dtTotal);
                }
                dtTotal += curr.getTime();
            }
        }
        return null;
    }

    /**
     * Method used to retrieve the maximum value in the Buffer given an index
     * The method explores in the range 0 <= index-1 <= index <= index+1 <= size
     * @param index - the index to start exploring at
     * @return the closest maximum value to the position - index, in the Buffer
     */
    public double closestMax(int index){
        if(index < 0 || index >= size){
            return Double.NaN;
        }

        if(index == size - 1){
            double curr = (double) this.toArray()[index];
            double prev = (double) this.toArray()[index-1];
            return (Math.max(curr, prev));
        }

        if(index == 0){
            double curr = (double) this.toArray()[index];
            double next = (double) this.toArray()[index+1];
            return (Math.max(curr,next));
        }

        double curr = (double) this.toArray()[index];
        double prev = (double) this.toArray()[index - 1];
        double next = (double) this.toArray()[index + 1];

        return Math.max(prev, Math.max(curr, next));
    }

}