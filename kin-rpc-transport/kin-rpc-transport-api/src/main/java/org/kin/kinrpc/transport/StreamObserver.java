package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2023/5/30
 */
public interface StreamObserver<V> {
    // TODO: 2023/6/1 
    void next(V value);

    void error(Throwable t);

    void complete();
}
