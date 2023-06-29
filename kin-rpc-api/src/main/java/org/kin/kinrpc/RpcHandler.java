package org.kin.kinrpc;

import org.kin.framework.proxy.ProxyInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2023/6/29
 */
public class RpcHandler {
    private static final Logger log = LoggerFactory.getLogger(RpcHandler.class);

    /** 服务方法元数据 */
    private final MethodMetadata metadata;
    /** 服务方法invoker */
    private final ProxyInvoker<?> invoker;

    public RpcHandler(MethodMetadata metadata, ProxyInvoker<?> invoker) {
        this.metadata = metadata;
        this.invoker = invoker;
    }

    /**
     * 服务方法调用
     *
     * @param params 方法调用参数
     * @return 方法调用返回值
     */
    public Object handle(Object... params) throws Exception {
        return invoker.invoke(params);
    }

    //getter
    public MethodMetadata metadata() {
        return metadata;
    }
}
