package com.example.cda.utils;

import java.util.concurrent.ArrayBlockingQueue;

public class CircularQueue<E> extends ArrayBlockingQueue<E> {

    private int size;

    public CircularQueue(int capacity) {
        super(capacity);
        this.size = capacity;
    }

    @Override
    public boolean add(E e) {
        // If FIFO queue is full, remove oldest element to make space for new one
        if(super.size() == this.size){
            this.remove();
        }
        return super.add(e);
    }

    public double[] getRecentPair(){
        return new double[]{(double) this.toArray()[this.size()-1], (double) this.toArray()[this.size()-2]};
    }

    public double recent(){
        if(this.size() == 0){
            return 0;
        }
        return (double) this.toArray()[this.size() - 1];
    }

    public int indexOf(E e){
        E[] tmp = (E[]) this.toArray();
        for(int i =0;i<this.size();i++){
            if(tmp[i] == e){
                return i;
            }
        }
        return Integer.MAX_VALUE;
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

        double curr = (double) this.toArray()[index];
        double prev = (double) this.toArray()[index - 1];
        double next = (double) this.toArray()[index + 1];

        return Math.max(prev, Math.max(curr, next));


    }
}