package org.kin.kinrpc.transport.http;

import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class JsonRpcProxyFactoryBean extends RemoteInvocationBasedAccessor
        implements MethodInterceptor,
        InitializingBean,
        FactoryBean<Object>,
        ApplicationContextAware {
    private final JsonProxyFactoryBean jsonProxyFactoryBean;

    public JsonRpcProxyFactoryBean(JsonProxyFactoryBean factoryBean) {
        this.jsonProxyFactoryBean = factoryBean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() {
        jsonProxyFactoryBean.afterPropertiesSet();
    }

    @Override
    public Object invoke(MethodInvocation invocation)
            throws Throwable {

        return jsonProxyFactoryBean.invoke(invocation);
    }

    @Override
    public Object getObject() {
        return jsonProxyFactoryBean.getObject();
    }

    @Override
    public Class<?> getObjectType() {
        return jsonProxyFactoryBean.getObjectType();
    }

    @Override
    public boolean isSingleton() {
        return jsonProxyFactoryBean.isSingleton();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        jsonProxyFactoryBean.setApplicationContext(applicationContext);
    }

    @Override
    public void setServiceUrl(String serviceUrl) {
        jsonProxyFactoryBean.setServiceUrl(serviceUrl);
    }

    @Override
    public void setServiceInterface(Class<?> serviceInterface) {
        jsonProxyFactoryBean.setServiceInterface(serviceInterface);
    }
}
