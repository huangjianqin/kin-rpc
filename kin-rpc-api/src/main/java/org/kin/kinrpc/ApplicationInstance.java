package org.kin.kinrpc;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public interface ApplicationInstance {
    /**
     * 返回application instance group
     *
     * @return application instance group
     */
    String group();

    /**
     * 返回application instance host
     *
     * @return application instance host
     */
    String host();

    /**
     * 返回application instance port
     *
     * @return application instance port
     */
    int port();

    /**
     * 返回application instance metadata
     *
     * @return application instance metadata
     */
    Map<String, String> metadata();

    /**
     * 返回application instance schema
     *
     * @return application instance schema
     */
    String scheme();

    /**
     * 返回应用实例元数据
     *
     * @param key 元数据key
     * @return
     */
    default String metadata(String key) {
        return metadata(key, "");
    }

    /**
     * 返回应用实例元数据
     *
     * @param key          元数据key
     * @param defaultValue 缺省默认值
     * @return
     */
    default String metadata(String key, String defaultValue) {
        return metadata().getOrDefault(key, defaultValue);
    }
}
