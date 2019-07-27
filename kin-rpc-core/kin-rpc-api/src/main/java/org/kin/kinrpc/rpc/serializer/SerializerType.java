package org.kin.kinrpc.rpc.serializer;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.serializer.impl.Hessian2Serializer;
import org.kin.kinrpc.rpc.serializer.impl.JavaSerializer;
import org.kin.kinrpc.rpc.serializer.impl.KryoSerializer;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializerType {
    /**
     * java自带序列化
     */
    JAVA("java") {
        private final Serializer instance = new JavaSerializer();

        @Override
        public Serializer newInstance() {
            return instance;
        }
    },
    /**
     * kryo序列化
     */
    KRYO("kryo") {
        private final Serializer instance = new KryoSerializer();

        @Override
        public Serializer newInstance() {
            return instance;
        }
    },
    /**
     * hession序列化
     */
    HESSION("hession") {
        private final Serializer instance = new Hessian2Serializer();

        @Override
        public Serializer newInstance() {
            return instance;
        }
    },;

    SerializerType(String name) {
        this.name = name;
    }

    private String name;

    public abstract Serializer newInstance();

    public String getName() {
        return name;
    }

    private static SerializerType[] types = values();

    public static SerializerType getByName(String name) {
        if (StringUtils.isNotBlank(name)) {
            name = name.toLowerCase();
            for (SerializerType type : types) {
                if (type.getName().equals(name)) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("unknown serialize type '" + name + "'");
    }
}
