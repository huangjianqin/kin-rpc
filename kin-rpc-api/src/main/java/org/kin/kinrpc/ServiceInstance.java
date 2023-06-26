package org.kin.kinrpc;

import java.util.Map;

/**
 * service信息
 * todo 记得实现hashcode equal
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

    /**
     * 返回服务权重
     *
     * @return 服务权重
     */
    int weight();
}
