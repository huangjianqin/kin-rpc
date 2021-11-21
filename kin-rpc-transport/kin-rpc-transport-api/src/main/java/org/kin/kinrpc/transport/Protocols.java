package org.kin.kinrpc.transport;

import org.kin.framework.utils.AbstractExtensionCache;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
public final class Protocols extends AbstractExtensionCache<String, Protocol> {
    //单例
    public static final Protocols INSTANCE = new Protocols();

    public Protocols() {
        super(RpcServiceLoader.LOADER);
    }

    @Override
    protected String[] keys(Protocol protocol) {
        Class<? extends Protocol> claxx = protocol.getClass();
        return new String[]{ClassUtils.getPrefixName(claxx, Protocol.class), claxx.getName(), claxx.getSimpleName()};
    }
}
