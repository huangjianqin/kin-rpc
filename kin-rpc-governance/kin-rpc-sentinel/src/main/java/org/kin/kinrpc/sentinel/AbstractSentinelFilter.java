package org.kin.kinrpc.sentinel;

import org.kin.kinrpc.Filter;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
public abstract class AbstractSentinelFilter implements Filter {
    @Override
    public int order() {
        return HIGH_ORDER_4;
    }
}
