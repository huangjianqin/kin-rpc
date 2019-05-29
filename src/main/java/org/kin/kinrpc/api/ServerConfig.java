package org.kin.kinrpc.api;

import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.remoting.transport.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServerConfig {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    private String host = "127.0.0.1";
    private final int port;
    private int threadNum = Constants.SERVER_DEFAULT_THREADNUM;

    //底层通信服务器
    private Server server;

    public ServerConfig(int port) {
        this.port = port;
    }

    public ServerConfig() {
        this.port = Constants.SERVER_DEFAULT_PORT;
//        try {
//            host = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[0].getHostAddress();
//        } catch (UnknownHostException e) {
//            log.error("get localhost error!!!");
//            e.printStackTrace();
//        }
    }

    /**
     * 检查配置参数正确性
     */
    private void checkConfig() {
        if (port < 0) {
            throw new IllegalStateException("Server's port must be greater than 0");
        }

        if (threadNum <= 0) {
            throw new IllegalStateException("Server thread's num must greater than 0");
        }
    }

    /**
     * config包下的类才可以调用此方法
     *
     * @return
     */
    Server getServer() {
        checkConfig();

        log.info("server config >>>");
        log.info("host= " + this.host);
        log.info("post= " + this.port);
        log.info("threadNum= " + this.threadNum);
        log.info("<<<");
        log.info("getting Server...");

        if (this.server == null) {
            synchronized (this) {
                if (this.server == null) {
                    log.info("ready to start Server...");
                    this.server = new Server(this);
                    this.server.start();
                } else {
                    log.info("server with certain port '" + port + "' has started");
                    log.info("reuse server...");
                }
            }
        } else {
            log.info("server with certain port '" + port + "' has started");
            log.info("reuse server...");
        }
        return server;
    }

    public int getPort() {
        return port;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        if (this.server != null) {
            synchronized (this) {
                if (this.server != null) {
                    this.server.setMaxThreadsNum(this.threadNum);
                }
            }
        }
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerConfig)) return false;

        ServerConfig that = (ServerConfig) o;

        return port == that.port;

    }

    @Override
    public int hashCode() {
        return port;
    }
}
