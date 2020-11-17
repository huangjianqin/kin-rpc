package org.kin.kinrpc.transport.http;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;

import java.lang.reflect.InvocationTargetException;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class HttpRemoteInvocation extends RemoteInvocation {
    private static final long serialVersionUID = -8035743620933401906L;

    public HttpRemoteInvocation(MethodInvocation methodInvocation) {
        super(methodInvocation);
    }

    @Override
    public Object invoke(Object targetObject) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        return super.invoke(targetObject);
    }
}
