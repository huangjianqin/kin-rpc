package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.config.ReferenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 失败安全, 即打印报错日志, 然后返回默认值
 *
 * @author huangjianqin
 * @date 2023/7/29
 */
@Extension("failsafe")
public class FailSafeClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FailSafeClusterInvoker.class);

    public FailSafeClusterInvoker(ReferenceConfig<T> config) {
        super(config);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        selectAttachOrThrow(invocation, Collections.emptyList());
        RpcResult rpcResult = invokeFilterChain(invocation);
        rpcResult.onFinish((r, t) -> {
            if (Objects.isNull(t)) {
                //success
                future.complete(r);
            } else {
                //fail
                log.error("failsafe cluster rpc call fail, ignore error and return default value", t);
                //返回默认值
                future.complete(ClassUtils.getDefaultValue(invocation.returnType()));
            }
        });
    }
}
