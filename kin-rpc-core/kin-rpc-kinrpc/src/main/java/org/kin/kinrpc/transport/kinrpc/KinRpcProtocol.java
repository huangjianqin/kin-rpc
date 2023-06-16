package org.kin.kinrpc.transport.kinrpc;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.*;
import org.kin.kinrpc.rpc.*;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.common.constants.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.executor.ExecutorFactory;
import org.kin.kinrpc.rpc.config.ExecutorType;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.transport.NettyUtils;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
@Extension("kinrpc")
public final class KinRpcProtocol implements Protocol, LoggerOprs {
    static {
        ProtocolFactory.init(KinRpcRequestProtocol.class.getPackage().getName());
    }

    private static final Cache<Integer, KinRpcProvider> PROVIDER_CACHE = CacheBuilder.newBuilder().build();

    @Override
    public <T> Exporter<T> export(ProviderInvoker<T> invoker) {
        Url url = invoker.url();

        String host = url.getHost();
        int port = url.getPort();
        int serializationType = url.getIntParam(Constants.SERIALIZATION_KEY);

        Serialization serialization = ExtensionLoader.getExtension(Serialization.class, serializationType);
        Preconditions.checkNotNull(serialization, "unvalid serialization type: [" + serializationType + "]");

        int compression = url.getIntParam(Constants.COMPRESSION_KEY);
        CompressionType compressionType = CompressionType.getById(compression);
        Preconditions.checkNotNull(serialization, "unvalid compression type: id=[" + compression + "]");

        KinRpcProvider provider = null;
        try {
            provider = PROVIDER_CACHE.get(port, () -> {
                String executorFactoryType = url.getParam(Constants.EXECUTOR_KEY);
                ExecutorFactory executorFactory;
                if (StringUtils.isNotBlank(executorFactoryType)) {
                    executorFactory = ExecutorType.getByName(executorFactoryType).create(url, port);
                } else {
                    //default
                    executorFactory = ExecutorType.EAGER.create(url, port);
                }

                KinRpcProvider provider0 = new KinRpcProvider(host, port, executorFactory, serialization, compressionType, NettyUtils.convert(url));
                try {
                    provider0.bind();
                } catch (Exception e) {
                    provider0.shutdown();
                    throw e;
                }

                return provider0;
            });
            if (!provider.getSerialization().equals(serialization)) {
                throw new IllegalStateException(String.format("origin server(port=%s) serialization type is not equals %s", port, serializationType));
            }
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        Exporter<T> exporter = null;
        try {
            provider.addService(invoker);
            exporter = new KinRpcExporter<>((invoker));
        } catch (Exception e) {
            unexport(url);
            ExceptionUtils.throwExt(e);
        }

        info("kinrpc service '{}' export address '{}'", url.getServiceKey(), NetUtils.getIpPort(host, port));

        return exporter;
    }

    /**
     * 取消监听rpc消息, 如果该port没有任何服务注册, shutdown server
     */
    private void unexport(Url url) {
        KinRpcProvider provider = PROVIDER_CACHE.getIfPresent(url.getPort());
        if (Objects.nonNull(provider)) {
            provider.disableService(url);
            provider.idleShutdown(() -> PROVIDER_CACHE.invalidate(url.getPort()));
        }
    }

    @Override
    public <T> AsyncInvoker<T> refer(Url url) {
        info("kinrpc reference '{}' refer address '{}'", url.getAddress());
        return new KinRpcReferenceInvoker<>(url);
    }

    @Override
    public void destroy() {
        for (KinRpcProvider provider : PROVIDER_CACHE.asMap().values()) {
            provider.shutdown();
        }
    }

    //-------------------------------------------------------------------------------------------------------------------------
    private class KinRpcExporter<T> implements Exporter<T> {
        /** 包装的provider invoker */
        private final ProviderInvoker<T> providerInvoker;

        public KinRpcExporter(ProviderInvoker<T> providerInvoker) {
            this.providerInvoker = providerInvoker;
        }

        @Override
        public Invoker<T> getInvoker() {
            return providerInvoker;
        }

        @Override
        public void unexport() {
            //销毁provider invoker
            providerInvoker.destroy();

            KinRpcProtocol.this.unexport(getInvoker().url());
        }
    }
}
