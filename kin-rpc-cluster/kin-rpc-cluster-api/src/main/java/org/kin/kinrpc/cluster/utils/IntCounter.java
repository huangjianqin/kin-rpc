package org.kin.kinrpc.cluster.utils;

/**
 * @author huangjianqin
 * @date 2019-09-10
 */
public class IntCounter {
    private int number = 0;

    public IntCounter() {
    }

    public IntCounter(int number) {
        this.number = number;
    }

    public void increment(){
        number++;
    }

    public int getCount() {
        return number;
    }
}
