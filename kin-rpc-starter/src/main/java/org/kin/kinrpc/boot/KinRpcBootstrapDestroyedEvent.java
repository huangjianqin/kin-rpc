package org.kin.kinrpc.boot;

import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.springframework.context.ApplicationEvent;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
public class KinRpcBootstrapDestroyedEvent extends ApplicationEvent {
    public KinRpcBootstrapDestroyedEvent(KinRpcBootstrap bootstrap) {
        super(bootstrap);
    }

    //getter
    public KinRpcBootstrap getKinRpcBootstrap() {
        return (KinRpcBootstrap) super.getSource();
    }
}