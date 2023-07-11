package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.ServiceConfig;
import org.springframework.context.ApplicationEvent;

/**
 * kinrpc service exported event
 * todo
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public final class ServiceExportedEvent extends ApplicationEvent {
    /** 服务配置 */
    private final ServiceConfig<?> serviceConfig;

    public ServiceExportedEvent(ServiceConfig serviceConfig, Object source) {
        super(source);
        this.serviceConfig = serviceConfig;
    }

    //getter
    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }
}
