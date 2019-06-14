package org.kin.kinrpc.rpc.invoker;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class ProviderInvoker extends AbstractInvoker {
    protected static final Logger log = LoggerFactory.getLogger("invoker");

    //服务类
    protected Object serivce;
    protected Map<String, Method> methodMap = new HashMap<String, Method>();

    public ProviderInvoker(Class<?> interfaceClass) {
        super(interfaceClass);
    }
}
