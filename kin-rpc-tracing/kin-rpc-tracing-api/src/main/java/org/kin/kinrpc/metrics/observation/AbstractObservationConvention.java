package org.kin.kinrpc.metrics.observation;

import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import io.micrometer.common.lang.Nullable;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Invocation;

import static org.kin.kinrpc.metrics.observation.KinRpcObservationDocumentation.LowCardinalityKeyNames.*;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public abstract class AbstractObservationConvention {
    private static final String PATH_SEPARATOR = "/";

    /**
     * return default observation kvs
     */
    protected KeyValues getLowCardinalityKeyValues(Invocation invocation) {
        //带上rpc system name tag
        KeyValues keyValues = KeyValues.of(RPC_SYSTEM.withValue("kin.kinrpc"));
        String serviceName = invocation.serviceName();
        //带上service name tag
        keyValues = appendNonNull(keyValues, RPC_SERVICE, serviceName);
        //带上handler name tag
        return appendNonNull(keyValues, RPC_METHOD, invocation.handlerName());
    }

    /**
     * 如果{@code value}不为null, 则添加kv到{@code keyValues}
     */
    protected KeyValues appendNonNull(KeyValues keyValues, KeyName keyName, @Nullable String value) {
        if (StringUtils.isNotBlank(value)) {
            return keyValues.and(keyName.withValue(value));
        }
        return keyValues;
    }

    /**
     * return default observation context name
     */
    protected String getContextualName(Invocation invocation) {
        String serviceName = invocation.serviceName();
        String methodName = invocation.handlerName();
        String method = StringUtils.isNotBlank(methodName) ? methodName : "";
        return serviceName + PATH_SEPARATOR + method;
    }
}
