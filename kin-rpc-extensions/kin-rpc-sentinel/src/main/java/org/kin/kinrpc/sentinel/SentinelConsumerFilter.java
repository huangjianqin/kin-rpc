package org.kin.kinrpc.sentinel;

import com.alibaba.csp.sentinel.*;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.kin.kinrpc.*;
import org.kin.kinrpc.constants.Scopes;
import org.kin.kinrpc.fallback.FallbackManager;
import org.kin.kinrpc.sentinel.utils.SentinelUtils;

import java.util.LinkedList;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
@Scope(Scopes.CONSUMER)
public class SentinelConsumerFilter extends AbstractSentinelFilter {
    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        LinkedList<EntryHolder> queue = new LinkedList<>();
        String interfaceResName = SentinelUtils.getInterfaceResName(invocation, SentinelUtils.getConsumerResNamePrefix());
        String methodResName = SentinelUtils.getMethodResName(invocation, SentinelUtils.getConsumerResNamePrefix());
        try {
            queue.push(new EntryHolder(
                    SphU.asyncEntry(interfaceResName, ResourceTypeConstants.COMMON_RPC, EntryType.OUT), null));
            queue.push(new EntryHolder(
                    SphU.asyncEntry(methodResName, ResourceTypeConstants.COMMON_RPC,
                            EntryType.OUT, 1, invocation.params()), invocation.params()));
            //call
            RpcResult result = invoker.invoke(invocation);
            result.onFinish((r, t) -> {
                while (!queue.isEmpty()) {
                    EntryHolder holder = queue.pop();
                    Tracer.traceEntry(t, holder.entry);
                    holder.exit();
                }
            });
            return result;
        } catch (BlockException e) {
            while (!queue.isEmpty()) {
                queue.pop().exit();
            }
            return FallbackManager.onFallback(invocation,
                    new RpcExceptionBlockException(String.format("rpc call blocked, invocation=%s", invocation), e));
        }
    }

    @Override
    public int order() {
        return -1000;
    }

    //------------------------------------------------------------------------------------------------------------------------------

    /** {@link Entry} holder */
    private static class EntryHolder {
        /** sentinel entry */
        private final Entry entry;
        /** rpc call params */
        private final Object[] params;

        private EntryHolder(Entry entry, Object[] params) {
            this.entry = entry;
            this.params = params;
        }

        /**
         * sentinel entry exit
         */
        public void exit() {
            if (params != null) {
                entry.exit(1, params);
            } else {
                entry.exit();
            }
        }
    }
}
