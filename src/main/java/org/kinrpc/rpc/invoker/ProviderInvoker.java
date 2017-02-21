package org.kinrpc.rpc.invoker;

import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 健勤 on 2017/2/14.
 */
public abstract class ProviderInvoker extends AbstractInvoker {
    private static final Logger log = Logger.getLogger(JavaProviderInvoker.class);

    //服务类
    protected Object serivce;
    protected Map<String, Method> methodMap = new HashMap<String, Method>();

    public ProviderInvoker(Class<?> interfaceClass) {
        super(interfaceClass);
    }


}
