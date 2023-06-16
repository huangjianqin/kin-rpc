package org.kin.kinrpc.cluster;

import org.kin.kinrpc.rpc.RpcCallReturnAdapter;

/**
 * 默认处理rpc call return逻辑
 *
 * @author huangjianqin
 * @date 2021/2/6
 */
class DefaultReturnAdapter implements RpcCallReturnAdapter {
    static final DefaultReturnAdapter INSTANCE = new DefaultReturnAdapter();

    private DefaultReturnAdapter() {
    }

    @Override
    public boolean match(Class<?> returnType) {
        return true;
    }
}
