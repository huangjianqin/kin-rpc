package org.kin.kinrpc;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.ServiceConfig;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
@SPI(alias = "serviceListener")
public interface ServiceListener {
    /**
     * service export后触发
     *
     * @param serviceConfig 服务配置
     */
    void onExported(ServiceConfig<?> serviceConfig);

    /**
     * service unExport后触发
     *
     * @param serviceConfig 服务配置
     */
    void onUnExported(ServiceConfig<?> serviceConfig);
}
