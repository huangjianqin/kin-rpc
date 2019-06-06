package org.kin.kinrpc.future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncRPCCallback {
    void success(Object result);

    void fail(Exception e);
}
