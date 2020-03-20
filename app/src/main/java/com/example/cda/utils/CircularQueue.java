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


    public int indexOf(double e){
        Iterator it = this.iterator();
        int index = 0;
        while(it.hasNext()){
            if(it.next().equals(e)){
                return index;
            }
            index++;
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