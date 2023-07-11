package org.kin.kinrpc.config;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.utils.ObjectUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * server配置
 * todo kinrpc attachment netty options
 * todo grpc attachment Inbound消息大小, Inbound元数据大小, 流控窗口, 每个连接最大请求并发处理
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ServerConfig extends SharableConfig<ServerConfig> {
    /** 默认id生成 */
    private static final AtomicInteger DEFAULT_ID_GEN = new AtomicInteger();
    /** 默认id前缀 */
    private static final String DEFAULT_ID_PREFIX = "$" + ServerConfig.class.getSimpleName() + "-";

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

    /**
     * 复用{@link ServerConfig}实例
     *
     * @param id server config id
     * @return {@link ServerConfig}实例
     */
    public static ServerConfig fromId(String id) {
        return new ServerConfig().id(id);
    }

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

    @Override
    protected String genDefaultId() {
        return DEFAULT_ID_PREFIX + DEFAULT_ID_GEN.incrementAndGet();
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

    public Integer getPort() {
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

    //----------------------
    public ServerConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public ServerConfig setHost(String host) {
        this.host = host;
        return this;
    }

    public ServerConfig setPort(Integer port) {
        this.port = port;
        return this;
    }

    public ServerConfig setExecutor(ExecutorConfig executor) {
        this.executor = executor;
        return this;
    }

    public ServerConfig setSsl(SslConfig ssl) {
        this.ssl = ssl;
        return this;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                super.toString() +
                ", protocol='" + protocol + '\'' +
                ObjectUtils.toStringIfPredicate(StringUtils.isNotBlank(host), ", host='" + host + '\'') +
                ", port=" + port +
                ObjectUtils.toStringIfNonNull(executor, ", executor=" + executor) +
                ObjectUtils.toStringIfNonNull(ssl, ", ssl=" + ssl) +
                '}';
    }
}
