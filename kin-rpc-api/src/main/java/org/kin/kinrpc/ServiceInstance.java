package org.kin.kinrpc;

import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.utils.GsvUtils;

import java.util.Map;

/**
 * service信息
 * !!!其实现类也必须要实现{@link Object#equals(Object)}和{@link Object#hashCode()}
 *
 * @author huangjianqin
 * @date 2023/6/24
 */
public interface ServiceInstance {
    /**
     * 返回服务唯一id
     *
     * @return 服务唯一id
     */
    default int serviceId() {
        return GsvUtils.serviceId(service());
    }

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    String service();

    /**
     * 返回service instance host
     *
     * @return service instance host
     */
    String host();

    /**
     * 返回service instance port
     *
     * @return service instance port
     */
    int port();

    /**
     * 返回service instance address
     *
     * @return service instance address
     */
    default String address() {
        return NetUtils.getIpPort(host(), port());
    }

    /**
     * 返回service instance metadata
     *
     * @return service instance metadata
     */
    Map<String, String> metadata();

    /**
     * 返回service instance schema
     *
     * @return service instance schema
     */
    String scheme();

    /**
     * 返回服务权重
     *
     * @return 服务权重
     */
    int weight();

    /**
     * 返回服务元数据
     *
     * @param key 元数据key
     * @return
     */
    default String metadata(String key) {
        return metadata(key, "");
    }

    /**
     * 返回服务元数据
     *
     * @param key          元数据key
     * @param defaultValue 缺省默认值
     * @return
     */
    default String metadata(String key, String defaultValue) {
        return metadata().getOrDefault(key, defaultValue);
    }
}
