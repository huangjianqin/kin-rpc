package org.kin.kinrpc.demo.rpc;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020/11/13
 */
public class Return1 implements Serializable {
    private static final long serialVersionUID = -827382375516066392L;

    @Override
    public String toString() {
        return Thread.currentThread().getName().concat(" notify rpc call return");
    }
}
