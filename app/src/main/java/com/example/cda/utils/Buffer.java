package com.example.cda.utils;

import java.util.ArrayDeque;
import java.util.Iterator;

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

    public double[] getRecentPair(){
        if(super.size() < 2){
            return null;
        }
        return new double[]{(double) this.toArray()[0], (double) this.toArray()[1]};
    }

    public E recent(){
        if(super.size() == 0){
            return null;
        }

        return super.getFirst();
    }

    public E indexOf(E e){
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
                dtTotal += curr.getdT();
            }
        }
        if(e instanceof Double){
            Iterator it = this.iterator();
            int index = 0;
            while(it.hasNext()){
                double curr = (double) it.next();
                if(curr == (Double) e){
                    return (E) Integer.valueOf(index);
                }
                index++;
            }
        }

        return null;
    }

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