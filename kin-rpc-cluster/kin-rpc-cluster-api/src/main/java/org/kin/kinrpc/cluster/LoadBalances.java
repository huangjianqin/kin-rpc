package org.kin.kinrpc.cluster;

import org.kin.framework.utils.AbstractExtensionCache;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public final class LoadBalances extends AbstractExtensionCache<String, LoadBalance> {
    //单例
    public static final LoadBalances INSTANCE = new LoadBalances();

    public LoadBalances() {
        super(RpcServiceLoader.LOADER);
    }

    @Override
    protected String[] keys(LoadBalance loadBalance) {
        Class<? extends LoadBalance> claxx = loadBalance.getClass();
        return new String[]{ClassUtils.getPrefixName(claxx, LoadBalance.class), claxx.getName(), claxx.getSimpleName()};
    }
}
