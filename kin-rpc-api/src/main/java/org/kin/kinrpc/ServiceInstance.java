package org.kin.kinrpc;

import java.util.Map;

/**
 * service信息
 *
 * @author huangjianqin
 * @date 2023/6/24
 */
public interface ServiceInstance {
    /**
     * 返回serviceId
     *
     * @return serviceId
     */
    String getServiceId();

    /**
     * 返回service host
     *
     * @return service host
     */
    String getHost();

    /**
     * 返回service host
     *
     * @return service port
     */
    int getPort();

    /**
     * 返回service metadata
     *
     * @return service metadata
     */
    Map<String, String> getMetadata();

    /**
     * 返回service schema
     *
     * @return service schema
     */
    String getScheme();
}
