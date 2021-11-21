package org.kin.kinrpc.serialization;

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
    private static void test(Serialization serialization, Consumer<byte[]> afterSerialize, BiFunction<Message, Message, Void> afterDeserialize) throws IOException, ClassNotFoundException {
        Message origin = new Message(1, "aa", new Message(2, "empty", null));
        byte[] bytes = serialization.serialize(origin);
        if (Objects.nonNull(afterSerialize)) {
            afterSerialize.accept(bytes);
        }
        Message deserialize = serialization.deserialize(bytes, Message.class);
        if (Objects.nonNull(afterDeserialize)) {
            afterDeserialize.apply(origin, deserialize);
        }
        System.out.println(deserialize);
    }

    //-----------------------------builder-------------------------------------
    public static Builder builder(SerializationType type) {
        return new Builder(type);
    }

    public static Builder builder(Serialization serialization) {
        return new Builder(serialization);
    }

    public static class Builder {
        private final Serialization serialization;
        private Consumer<byte[]> afterSerialize;
        private BiFunction<Message, Message, Void> afterDeserialize;

        public Builder(SerializationType type) {
            this(Serializations.INSTANCE.getExtension(type.getCode()));
        }

        public Builder(Serialization serialization) {
            this.serialization = serialization;
        }

        public Builder afterSerialize(Consumer<byte[]> afterSerialize) {
            this.afterSerialize = afterSerialize;
            return this;
        }

        public Builder afterDeserialize(BiFunction<Message, Message, Void> afterDeserialize) {
            this.afterDeserialize = afterDeserialize;
            return this;
        }

        public void run() {
            try {
                test(serialization, afterSerialize, afterDeserialize);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
