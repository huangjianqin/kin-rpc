package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface RemotingServer {
    /**
     * start the server
     */
    boolean start();

    /**
     * shutdown the server
     */
    boolean shutdown();

    /**
     * 获取server的host
     * @return host
     */
    String host();

    /**
     * 获取server监听端口
     * @return listened port
     */
    int port();

    /**
     * 注册request processor
     * @param processor request processor
     */
    void registerUserProcessor(RequestProcessor<?> processor);
}
