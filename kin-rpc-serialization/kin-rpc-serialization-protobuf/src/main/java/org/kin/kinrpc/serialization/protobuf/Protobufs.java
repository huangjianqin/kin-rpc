package org.kin.kinrpc.serialization.protobuf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.kin.kinrpc.serialization.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public final class Protobufs {
    private Protobufs() {
    }

    /** parser缓存 */
    private static final ConcurrentMap<Class<? extends MessageLite>, MessageMarshaller<?>> MARSHALLERS = new ConcurrentHashMap<>();
    /** 获取{@link GeneratedMessageV3.Builder}的{@link Method}缓存 */
    private static final Cache<Class<?>, Method> BUILDER_CACHE = CacheBuilder.newBuilder().build();

    private static final ExtensionRegistryLite GLOBAL_REGISTRY = ExtensionRegistryLite.getEmptyRegistry();

    /**
     * !!!必须手动注册class parser
     */
    public static <T extends MessageLite> void register(T defaultInstance) {
        MARSHALLERS.put(defaultInstance.getClass(), new MessageMarshaller<>(defaultInstance));
    }

    /**
     * protobuf serialize
     */
    public static byte[] serialize(Object target) throws IOException {
        if (!(target instanceof MessageLite)) {
            throw new SerializationException(target.getClass().getName().concat("is not a protobuf object"));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        MessageLite messageLite = (MessageLite) target;
        messageLite.writeDelimitedTo(baos);

        return baos.toByteArray();
    }

    /**
     * protobuf deserialize
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes, Class<T> targetClass) throws InvalidProtocolBufferException {
        if (!MessageLite.class.isAssignableFrom(targetClass)) {
            throw new SerializationException(targetClass.getName().concat("is not a protobuf object"));
        }
        MessageMarshaller<?> marshaller = MARSHALLERS.get(targetClass);
        if (marshaller == null) {
            throw new SerializationException(targetClass.getName().concat("does not register"));
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        return (T) marshaller.parse(bais);
    }

    /**
     * protobuf deserialize to json
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T deserializeJson(String json, Class<T> messageClass) throws InvalidProtocolBufferException {
        GeneratedMessageV3.Builder builder;
        try {
            builder = getMessageBuilder(messageClass);
        } catch (Exception e) {
            throw new SerializationException("get google protobuf message builder from " + messageClass.getName() + "failed", e);
        }
        JsonFormat.parser().merge(json, builder);
        return (T) builder.build();
    }

    /**
     * protobuf serialize to json
     */
    public static String serializeJson(Object value) throws InvalidProtocolBufferException {
        JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        return printer.print((MessageOrBuilder) value);
    }

    /**
     * 获取MessageBuilder
     */
    @SuppressWarnings("rawtypes")
    private static GeneratedMessageV3.Builder getMessageBuilder(Class<?> requestType) throws Exception {
        Method method = BUILDER_CACHE.get(requestType, () -> requestType.getMethod("newBuilder"));
        return (GeneratedMessageV3.Builder) method.invoke(null);
    }

    //------------------------------------------------------------------------------------------------------------

    /**
     * protobuf message 封装
     */
    private static final class MessageMarshaller<T extends MessageLite> {
        /** parser */
        private final Parser<T> parser;
        /** protobuf默认实例 */
        private final T defaultInstance;

        @SuppressWarnings("unchecked")
        MessageMarshaller(T defaultInstance) {
            this.defaultInstance = defaultInstance;
            parser = (Parser<T>) defaultInstance.getParserForType();
        }

        @SuppressWarnings("unchecked")
        public Class<T> getMessageClass() {
            return (Class<T>) defaultInstance.getClass();
        }

        public T getMessageProtoInstance() {
            return defaultInstance;
        }

        public T parse(InputStream stream) throws InvalidProtocolBufferException {
            return parser.parseDelimitedFrom(stream, GLOBAL_REGISTRY);
        }
    }
}
