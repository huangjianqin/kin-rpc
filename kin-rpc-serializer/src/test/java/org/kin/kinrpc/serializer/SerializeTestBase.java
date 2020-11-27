package org.kin.kinrpc.serializer;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author huangjianqin
 * @date 2020/11/27
 */
public class SerializeTestBase {
    /**
     * 测试逻辑
     */
    private static void test(Serializer serializer, Consumer<byte[]> afterSerialize, BiFunction<Type, Type, Void> afterDeserialize) throws IOException, ClassNotFoundException {
        Type origin = new Type(1, "aa", new Type(2, "empty", null));
        byte[] bytes = serializer.serialize(origin);
        if (Objects.nonNull(afterSerialize)) {
            afterSerialize.accept(bytes);
        }
        Type deserialize = serializer.deserialize(bytes, Type.class);
        if (Objects.nonNull(afterDeserialize)) {
            afterDeserialize.apply(origin, deserialize);
        }
        System.out.println(deserialize);
    }

    //-----------------------------builder-------------------------------------
    public static Builder builder(SerializerType type) {
        return new Builder(type);
    }

    public static Builder builder(Serializer serializer) {
        return new Builder(serializer);
    }

    public static class Builder {
        private final Serializer serializer;
        private Consumer<byte[]> afterSerialize;
        private BiFunction<Type, Type, Void> afterDeserialize;

        public Builder(SerializerType type) {
            this(Serializers.getSerializer(type.getCode()));
        }

        public Builder(Serializer serializer) {
            this.serializer = serializer;
        }

        public Builder afterSerialize(Consumer<byte[]> afterSerialize) {
            this.afterSerialize = afterSerialize;
            return this;
        }

        public Builder afterDeserialize(BiFunction<Type, Type, Void> afterDeserialize) {
            this.afterDeserialize = afterDeserialize;
            return this;
        }

        public void run() {
            try {
                test(serializer, afterSerialize, afterDeserialize);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
