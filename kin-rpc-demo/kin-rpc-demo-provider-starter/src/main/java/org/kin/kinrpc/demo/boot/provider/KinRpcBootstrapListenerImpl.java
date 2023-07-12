package org.kin.kinrpc.demo.boot.provider;

import org.kin.kinrpc.boot.KinRpcBootstrapDestroyedEvent;
import org.kin.kinrpc.boot.KinRpcBootstrapStartedEvent;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.bootstrap.KinRpcBootstrapListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
@Component
public class KinRpcBootstrapListenerImpl implements KinRpcBootstrapListener, ApplicationListener<ApplicationEvent> {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof KinRpcBootstrapStartedEvent) {
            System.out.println("[event]bootstrap provider started");
        } else if (event instanceof KinRpcBootstrapDestroyedEvent) {
            System.out.println("[event]bootstrap provider destroyed");
        }
    }

    @Override
    public void onStarted(KinRpcBootstrap bootstrap) {
        System.out.println("[listener]bootstrap provider started");
    }

    @Override
    public void onDestroyed(KinRpcBootstrap bootstrap) {
        System.out.println("[listener]bootstrap provider destroyed");
    }
}
