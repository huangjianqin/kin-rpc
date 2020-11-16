package org.kin.kinrpc.serializer;

import org.kin.kinrpc.serializer.impl.JsonSerializer;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class JsonSerializerTest {
    public static void main(String[] args) throws IOException {
        JsonSerializer jsonSerializer = new JsonSerializer();
        String jsonStr = new String(jsonSerializer.serialize(new Type(1, "aa", new Type(0, "empty", null))));
        System.out.println(jsonStr);
        Type deserializeType = jsonSerializer.deserialize(jsonStr.getBytes(), Type.class);
        System.out.println(deserializeType);
    }

    private static class Type {
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
}
