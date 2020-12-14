package org.kin.kinrpc.demo.rpc;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020/11/19
 */
public class Return2 implements Serializable {
    private static final long serialVersionUID = -6725252721683610204L;

    @Override
    public String toString() {
        return Thread.currentThread().getName().concat("provider service async return");
    }
}
