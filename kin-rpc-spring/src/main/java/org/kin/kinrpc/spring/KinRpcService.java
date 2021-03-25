package org.kin.kinrpc.spring;

import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.serialization.SerializationType;
import org.kin.kinrpc.transport.ProtocolType;
import org.kin.transport.netty.CompressionType;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * 配置的含义请看{@link org.kin.kinrpc.config.ServiceConfig}
 *
 * @author huangjianqin
 * @date 2020/12/6
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface KinRpcService {
    String appName() default "";

    String host() default "0.0.0.0";

    int port() default 0;

    Class<?> interfaceClass();

    String serviceName() default "";

    String version() default "0.1.0.0";

    SerializationType serializationType() default SerializationType.KRYO;

    int serializationCode() default 0;

    boolean byteCodeEnhance() default true;

    boolean actorLike() default false;

    CompressionType compressionType() default CompressionType.NONE;

    boolean parallelism() default true;

    int tps() default Constants.PROVIDER_DEFAULT_TPS;

    ProtocolType protocolType() default ProtocolType.KINRPC;

    boolean ssl() default false;

    Attachment[] attachment() default {};
}
