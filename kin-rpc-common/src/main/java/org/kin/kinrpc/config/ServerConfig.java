package org.kin.kinrpc.config;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.utils.ObjectUtils;

import java.util.Objects;

/**
 * server配置
 * todo kinrpc attachment netty options
 * todo grpc attachment Inbound消息大小, Inbound元数据大小, 流控窗口, 每个连接最大请求并发处理
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ServerConfig extends AttachableConfig {
    /** 协议名 */
    private String protocol;
    /** 监听IP */
    private String host;
    /** 监听端口 */
    private Integer port;
    /** server端业务线程池大小, 默认是double cpu处理器数量, 队列长度为512的线程池 */
    private ExecutorConfig executor;
    /** 服务connection ssl配置 */
    private SslConfig ssl;

    public static ServerConfig create(String protocol) {
        return new ServerConfig().protocol(protocol);
    }

    public static ServerConfig create(String protocol, int port) {
        return create(protocol).port(port);
    }

    public static ServerConfig create(String protocol, String host, int port) {
        return create(protocol).host(host).port(port);
    }

    private static ServerConfig create(ProtocolType protocolType) {
        return create(protocolType.getName());
    }

    private static ServerConfig create(ProtocolType protocolType, int port) {
        return create(protocolType).port(port);
    }

    private static ServerConfig create(ProtocolType protocolType, String host, int port) {
        return create(protocolType).host(host).port(port);
    }

    public static ServerConfig kinrpc() {
        return create(ProtocolType.KINRPC);
    }

    public static ServerConfig kinrpc(int port) {
        return create(ProtocolType.KINRPC, port);
    }

    public static ServerConfig kinrpc(String host, int port) {
        return create(ProtocolType.KINRPC, host, port);
    }

    public static ServerConfig grpc() {
        return create(ProtocolType.GRPC);
    }

    public static ServerConfig grpc(int port) {
        return create(ProtocolType.GRPC, port);
    }

    public static ServerConfig grpc(String host, int port) {
        return create(ProtocolType.GRPC, host, port);
    }

    public static ServerConfig rsocket() {
        return create(ProtocolType.RSOCKET);
    }

    public static ServerConfig rsocket(int port) {
        return create(ProtocolType.RSOCKET, port);
    }

    public static ServerConfig rsocket(String host, int port) {
        return create(ProtocolType.RSOCKET, host, port);
    }

    private ServerConfig() {
    }

    @Override
    public void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(protocol), "server protocol must be not blank");
        check(StringUtils.isNotBlank(host), "server host must be not blank");
        check(port > 0, "server port must be greater than 0");
        if (Objects.nonNull(executor)) {
            executor.checkValid();
        }
    }

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (Objects.isNull(host)) {
            host = DefaultConfig.DEFAULT_SERVER_HOST;
        }

        if (Objects.isNull(port)) {
            port = DefaultConfig.DEFAULT_SERVER_PORT;
        }

        if (Objects.nonNull(executor)) {
            executor.initDefaultConfig();
        } else {
            executor = DefaultConfig.DEFAULT_SERVER_EXECUTOR;
        }
    }

    //setter && getter
    public String getProtocol() {
        return protocol;
    }

    public ServerConfig protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getHost() {
        return host;
    }

    public ServerConfig host(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ServerConfig port(int port) {
        this.port = port;
        return this;
    }

    public String getAddress() {
        return NetUtils.getIpPort(host, port);
    }

    public ExecutorConfig getExecutor() {
        return executor;
    }

    public ServerConfig executor(ExecutorConfig executor) {
        this.executor = executor;
        return this;
    }

    public SslConfig getSsl() {
        return ssl;
    }

    public ServerConfig ssl(SslConfig ssl) {
        this.ssl = ssl;
        return this;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "protocol='" + protocol + '\'' +
                ObjectUtils.toStringIfPredicate(StringUtils.isNotBlank(host), ", host='" + host + '\'') +
                ", port=" + port +
                ObjectUtils.toStringIfNonNull(executor, ", executor=" + executor) +
                ObjectUtils.toStringIfNonNull(ssl, ", ssl=" + ssl) +
                '}';
    }
}
