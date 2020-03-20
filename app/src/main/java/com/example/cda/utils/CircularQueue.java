package com.example.cda.utils;

import java.util.ArrayDeque;
import java.util.Iterator;

public class CircularQueue<E> extends ArrayDeque<E> {

    private int size;

    public CircularQueue(int capacity) {
        super(capacity);
        this.size = capacity;
    }

    @Override
    public boolean add(E e) {
        // If FIFO queue is full, remove oldest element to make space for new one
        if(super.size() == this.size){
            this.removeLast();
        }

        super.addFirst(e);
        return true;
    }

    public double[] getRecentPair(){
        return new double[]{(double) this.toArray()[0], (double) this.toArray()[1]};
    }

    public E recent(){
        if(this.size() == 0){
            return null;
        }

        return super.getFirst();
    }

    public double previous(){
        if(this.size() == 0){
            return 0;
        }
        return (double) this.toArray()[this.size() - 2];
    }


    public AccelerationTuple indexOf(E e){
        if(e instanceof AccelerationTuple) {
            Iterator it = this.iterator();
            int dtTotal = 0; // required to find how long deceleration occurred
            while (it.hasNext()) {
                AccelerationTuple curr = (AccelerationTuple) it.next();
                if (curr.getValue() == ((AccelerationTuple) e).getValue()) {
                    return new AccelerationTuple(curr.getValue(), dtTotal);
                }
                dtTotal += curr.getdT();
            }
        }

        return null;
    }

    public double closestMax(int index){
        if(index < 0 || index > size){
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