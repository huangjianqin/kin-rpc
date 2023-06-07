package org.kin.kinrpc.transport;

import org.kin.framework.utils.ClassUtils;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class AbsRequestProcessor<R extends Serializable> implements RequestProcessor<R>{
    @Override
    public final String interest() {
        return ClassUtils.getSuperInterfacesGenericRawTypes(RequestProcessor.class, getClass()).get(0).getName();
    }
}
