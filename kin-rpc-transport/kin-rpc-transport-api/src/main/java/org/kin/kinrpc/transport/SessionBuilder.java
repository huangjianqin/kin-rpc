package org.kin.kinrpc.transport;

/**
 * Created by huangjianqin on 2019/5/30.
 */
@FunctionalInterface
public interface SessionBuilder<C> {
    AbstractSession create(C channel);
}
