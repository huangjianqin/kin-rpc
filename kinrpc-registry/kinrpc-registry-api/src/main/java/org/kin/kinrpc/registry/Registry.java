package org.kin.kinrpc.registry;


import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2016/10/9.
 */

public interface Registry {
    void connect();

    void register(String serviceName, String host, int port);

    void unRegister(String serviceName, String host, int port);

    Directory subscribe(String serviceName, int connectTimeout);

    void unSubscribe(String serviceName);

    void retain();
    boolean release();

    void destroy();
}
