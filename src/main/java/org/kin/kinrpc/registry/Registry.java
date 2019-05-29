package org.kin.kinrpc.registry;

import org.kin.kinrpc.rpc.cluster.ZookeeperDirectory;

import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2016/10/9.
 */

public interface Registry {
    void connect() throws DataFormatException;

    void register(String serviceName, String host, int port) throws DataFormatException;

    void unRegister(String serviceName, String host, int port);

    ZookeeperDirectory subscribe(Class<?> interfaceClass, int connectTimeout);

    void destroy();
}
