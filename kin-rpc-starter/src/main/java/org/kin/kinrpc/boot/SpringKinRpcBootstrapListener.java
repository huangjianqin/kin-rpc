package org.kin.kinrpc.boot;

import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.bootstrap.KinRpcBootstrapListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
public class SpringKinRpcBootstrapListener implements KinRpcBootstrapListener, ApplicationContextAware {
    /** spring application context */
    private ApplicationContext applicationContext;

    @Override
    public void onStarted(KinRpcBootstrap bootstrap) {
        applicationContext.publishEvent(new KinRpcBootstrapStartedEvent(bootstrap));
    }

    @Override
    public void onDestroyed(KinRpcBootstrap bootstrap) {
        applicationContext.publishEvent(new KinRpcBootstrapDestroyedEvent(bootstrap));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
