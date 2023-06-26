package org.kin.kinrpc.utils;

import org.kin.framework.utils.MurmurHash3;

/**
 * 服务方法唯一标识工具类
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class HandlerUtils {
    private HandlerUtils() {
    }

    /**
     * 返回服务方法唯一标识
     *
     * @return 服务方法唯一标识
     */
    public static String handler(String group,
                                 String serviceName,
                                 String version,
                                 String handlerName) {
        return handler(GsvUtils.service(group, serviceName, version), handlerName);
    }

    /**
     * group/serviceName:version.handlerName
     *
     * @return 服务方法唯一标识
     */
    public static String handler(String service,
                                 String handlerName) {
        return service + "." + handlerName;
    }

    /**
     * 返回服务方法唯一id
     *
     * @return 服务方法唯一id, 即hash(服务方法唯一标识)
     */
    public static int handlerId(String group, String serviceName, String version, String handlerName) {
        return handlerId(handler(group, serviceName, version, handlerName));
    }

    /**
     * 返回服务方法唯一id
     *
     * @return 服务方法唯一id, 即hash(服务方法唯一标识)
     */
    public static int handlerId(String service, String handlerName) {
        return handlerId(handler(service, handlerName));
    }

    /**
     * 返回服务方法唯一id
     *
     * @param handler 服务方法唯一标识
     * @return 服务方法唯一id, 即hash(服务方法唯一标识)
     */
    public static int handlerId(String handler) {
        return MurmurHash3.hash32(handler);
    }
}
