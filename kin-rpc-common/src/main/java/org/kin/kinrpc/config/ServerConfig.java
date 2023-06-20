package org.kin.kinrpc.config;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.constants.Constants;

/**
 * todo TLS
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ServerConfig extends AbstractConfig {
    /** 协议名 */
    private String protocol;
    /** 监听IP */
    private String host = NetUtils.getLocalhostIp();
    /** 监听端口 */
    private int port = Constants.DEFAULT_SERVER_PORT;
    /** server端业务线程池大小, 默认是double cpu处理器数量, 无限队列长度的线程池 */
    private ExecutorConfig executor = ExecutorConfig.create(ExecutorType.FIX)
            .corePoolSize(SysUtils.DOUBLE_CPU)
            .maxPoolSize(SysUtils.DOUBLE_CPU)
            .queueSize(Integer.MAX_VALUE);
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
