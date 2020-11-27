package org.kin.kinrpc.serializer;

import java.io.Serializable;

/**
 * 序列化测试的类
 *
 * @author huangjianqin
 * @date 2020/11/27
 */
public class Type implements Serializable {
    private static final long serialVersionUID = -3399588049581257541L;

    private int a;
    private String b;
    private Object copy;

    public Type() {
    }

    public Type(int a, String b, Object copy) {
        this.a = a;
        this.b = b;
        this.copy = copy;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public Object getCopy() {
        return copy;
    }

    public void setCopy(Object copy) {
        this.copy = copy;
    }

    @Override
    public String toString() {
        return "Type{" +
                "a=" + a +
                ", b='" + b + '\'' +
                ", copy=" + copy +
                '}';
    }
}
