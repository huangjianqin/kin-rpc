package org.kin.kinrpc.spring;

import org.kin.kinrpc.conf.ServiceConfig;
import org.springframework.context.ApplicationEvent;

/**
 * kinrpc service exported event
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public final class ServiceExportedEvent extends ApplicationEvent {
    private final ServiceConfig serviceConfig;

    public ServiceExportedEvent(ServiceConfig serviceConfig, Object source) {
        super(source);
        this.serviceConfig = serviceConfig;
    }

    //getter

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }
}
