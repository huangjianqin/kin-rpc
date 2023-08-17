package org.kin.kinrpc.sentinel;

import com.alibaba.csp.sentinel.*;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.kin.kinrpc.*;
import org.kin.kinrpc.constants.Scopes;
import org.kin.kinrpc.fallback.FallbackManager;
import org.kin.kinrpc.sentinel.utils.SentinelUtils;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
@Scope(Scopes.PROVIDER)
public class SentinelProviderFilter extends AbstractSentinelFilter {
    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        //get origin caller
        String origin = SentinelUtils.getOriginParser().parse(invocation);
        if (null == origin) {
            origin = "";
        }

        Entry interfaceEntry = null;
        Entry methodEntry = null;
        String interfaceResName = SentinelUtils.getInterfaceResName(invocation, SentinelUtils.getProviderResNamePrefix());
        String methodResName = SentinelUtils.getMethodResName(invocation, SentinelUtils.getProviderResNamePrefix());
        try {
            // only need to create entrance context at provider side, as context will take effect
            // at entrance of invocation chain only (for inbound traffic).
            ContextUtil.enter(methodResName, origin);
            interfaceEntry = SphU.entry(interfaceResName, ResourceTypeConstants.COMMON_RPC, EntryType.IN);
            methodEntry = SphU.entry(methodResName, ResourceTypeConstants.COMMON_RPC, EntryType.IN,
                    invocation.params());
            //call
            RpcResult result = invoker.invoke(invocation);
            if (result.hasException()) {
                Tracer.traceEntry(result.getException(), interfaceEntry);
                Tracer.traceEntry(result.getException(), methodEntry);
            }
            return result;
        } catch (BlockException e) {
            return FallbackManager.onFallback(invocation,
                    new RpcExceptionBlockException(String.format("rpc call blocked, invocation=%s", invocation), e));
        } catch (RpcException e) {
            Tracer.traceEntry(e, interfaceEntry);
            Tracer.traceEntry(e, methodEntry);
            throw e;
        } finally {
            if (methodEntry != null) {
                methodEntry.exit(1, invocation.params());
            }
            if (interfaceEntry != null) {
                interfaceEntry.exit();
            }
            ContextUtil.exit();
        }
    }

    @Override
    public int order() {
        return -1000;
    }
}
