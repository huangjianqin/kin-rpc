package org.kinrpc.config;

import org.apache.log4j.Logger;
import org.kinrpc.remoting.transport.Server;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServerConfig {
    private static final Logger log = Logger.getLogger(ServerConfig.class);

    private String host = "127.0.0.1";
    private int port;
    private int threadNum = 16;

    //底层通信服务器
    private Server server;

    public ServerConfig() {
//        try {
//            host = InetAddress.getLocalHost().toString().split("\\\\")[1];
//        } catch (UnknownHostException e) {
//            log.error("get localhost error!!!");
//            e.printStackTrace();
//        }
    }

    public Server getServer() {
        log.info("server config >>>");
        log.info("host= " + this.host);
        log.info("post= " + this.port);
        log.info("threadNum= " + this.threadNum);
        log.info("<<<");
        log.info("getting Server...");
        if(this.server == null){
            synchronized (this){
                if(this.server == null){
                        log.info("ready to start Server...");
                        this.server = new Server(this);
                        this.server.start();
                }
                else{
                    log.info("server with certain port '" + port + "' has started");
                    log.info("reuse server...");
                }
            }
        }
        else{
            log.info("server with certain port '" + port + "' has started");
            log.info("reuse server...");
        }
        return server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
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
