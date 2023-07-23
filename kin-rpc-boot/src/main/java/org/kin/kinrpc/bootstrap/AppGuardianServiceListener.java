package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.ServiceListener;
import org.kin.kinrpc.config.ServiceConfig;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
public class AppGuardianServiceListener implements ServiceListener {
    @Override
    public void onExported(ServiceConfig<?> serviceConfig) {
        ApplicationGuardian guardian = ApplicationGuardian.instance();
        guardian.onServiceExported(serviceConfig);
        guardian.onStarted();
    }

    @Override
    public void onUnExported(ServiceConfig<?> serviceConfig) {
        //do nothing
    }
}
