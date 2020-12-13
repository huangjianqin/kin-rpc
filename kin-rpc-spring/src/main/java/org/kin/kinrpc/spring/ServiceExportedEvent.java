package org.kin.kinrpc.spring;

import org.springframework.context.ApplicationEvent;

/**
 * kinrpc service exported event
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public class ServiceExportedEvent extends ApplicationEvent {
    public ServiceExportedEvent(Object source) {
        super(source);
    }
}
