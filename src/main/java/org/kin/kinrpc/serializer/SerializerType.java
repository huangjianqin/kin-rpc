package org.kin.kinrpc.serializer;

import org.kin.framework.utils.StringUtils;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializerType {
    JAVA("java") {
        @Override
        public Serializer newInstance() {
            return null;
        }
    },
    KRYO("kryo") {
        @Override
        public Serializer newInstance() {
            return null;
        }
    },
    HESSION("hession") {
        @Override
        public Serializer newInstance() {
            return null;
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

        return null;
    }
}
