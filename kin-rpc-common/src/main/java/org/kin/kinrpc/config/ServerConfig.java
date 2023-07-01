package org.kin.kinrpc.config;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.constants.ServerConstants;

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
    /** jvm协议, 单例 */
    public static final ServerConfig JVM = ServerConfig.create(ProtocolType.JVM);

    /** 协议名 */
    private String protocol;
    /** 监听IP */
    private String host = NetUtils.getLocalhostIp();
    /** 监听端口 */
    private int port = ServerConstants.DEFAULT_SERVER_PORT;
    /** server端业务线程池大小, 默认是double cpu处理器数量, 队列长度为512的线程池 */
    private ExecutorConfig executor = ExecutorConfig.create(ExecutorType.FIX)
            .corePoolSize(SysUtils.CPU_NUM)
            .maxPoolSize(SysUtils.DOUBLE_CPU)
            .queueSize(1024);
    /** 服务connection ssl配置 */
    private SslConfig ssl;

    public static ServerConfig create() {
        return create(ProtocolType.KINRPC);
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

    public static ServerConfig create(ProtocolType protocolType) {
        return create(protocolType.getName());
    }

    public static ServerConfig create(ProtocolType protocolType, int port) {
        return create(protocolType).port(port);
    }

    public static ServerConfig create(ProtocolType protocolType, String host, int port) {
        return create(protocolType).host(host).port(port);
    }

    private ServerConfig() {
    }

    @Override
    protected void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(protocol), "server protocol must be not blank");
        if (!ProtocolType.JVM.getName().equalsIgnoreCase(protocol)) {
            //非jvm协议
            check(StringUtils.isNotBlank(host), "server host must be not blank");
            check(port > 0, "server port must be greater than 0");
        }
        if (Objects.nonNull(executor)) {
            executor.checkValid();
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
}
