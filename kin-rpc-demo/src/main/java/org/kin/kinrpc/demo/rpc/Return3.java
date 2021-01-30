package org.kin.kinrpc.demo.rpc;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2021/1/30
 */
public class Return3 implements Serializable {
    private static final long serialVersionUID = -555658782186325575L;

    @Override
    public String toString() {
        return Thread.currentThread().getName().concat("provider service return future");
    }
}
