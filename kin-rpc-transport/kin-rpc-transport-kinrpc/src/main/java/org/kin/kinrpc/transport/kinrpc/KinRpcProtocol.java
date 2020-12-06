package org.kin.kinrpc.transport.kinrpc;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Exporter;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.Serializers;
import org.kin.kinrpc.transport.Protocol;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
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
        int serializerType = url.getIntParam(Constants.SERIALIZE_KEY);

        Serializer serializer = Serializers.getSerializer(serializerType);
        Preconditions.checkNotNull(serializer, "unvalid serializer type: [" + serializerType + "]");

        int compression = url.getIntParam(Constants.COMPRESSION_KEY);
        CompressionType compressionType = CompressionType.getById(compression);
        Preconditions.checkNotNull(serializer, "unvalid compression type: id=[" + compression + "]");

        KinRpcProvider provider;
        try {
            provider = PROVIDER_CACHE.get(port, () -> {
                KinRpcProvider provider0 = new KinRpcProvider(host, port, serializer, compressionType);
                try {
                    provider0.bind();
                } catch (Exception e) {
                    provider0.shutdownNow();
                    throw e;
                }

                return provider0;
            });
            if (!provider.getSerializer().equals(serializer)) {
                throw new IllegalStateException(String.format("origin server(port=%s) serializer type is not equals %s", port, serializerType));
            }
        } catch (Exception e) {
            log().error(e.getMessage(), e);
            throw new RpcCallErrorException(e);
        }

        Exporter<T> exporter = null;
        try {
            provider.addService(invoker);
            exporter = new KinRpcExporter<>((invoker));
        } catch (Exception e) {
            log().error(e.getMessage(), e);
            unexport(url);
        }

        info("kinrpc service '{}' export address '{}'", NetUtils.getIpPort(host, port));

        return exporter;
    }

    /**
     * 取消监听rpc消息, 如果该port没有任何服务注册, shutdown server
     */
    private void unexport(Url url) {
        KinRpcProvider provider = PROVIDER_CACHE.getIfPresent(url.getPort());
        if (Objects.nonNull(provider)) {
            provider.disableService(url);
            provider.handle((p) -> {
                if (!p.isBusy()) {
                    //该端口没有提供服务, 关闭网络连接
                    p.shutdown();
                    PROVIDER_CACHE.invalidate(url.getPort());
                }
            });
        }
    }

    @Override
    public <T> AsyncInvoker<T> reference(Url url) {
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
