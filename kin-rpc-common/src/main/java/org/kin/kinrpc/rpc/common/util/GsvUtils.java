package org.kin.kinrpc.rpc.common.util;

import io.micrometer.core.instrument.util.StringUtils;

/**
 * group service version工具类
 * @author huangjianqin
 * @date 2023/2/25
 */
public final class GsvUtils {
    private GsvUtils() {
    }

    /**
     * group/service:version
     *
     * @return 服务唯一标识
     */
    public static String serviceKey(String group, String service, String version) {
        int length = service == null ? 0 : service.length();
        length += group == null ? 0 : group.length();
        length += version == null ? 0 : version.length();
        length += 2;
        StringBuilder buf = new StringBuilder(length);
        if (StringUtils.isNotEmpty(group)) {
            buf.append(group).append('/');
        }
        buf.append(service);
        if (StringUtils.isNotEmpty(version)) {
            buf.append(':').append(version);
        }
        return buf.toString().intern();
    }

    /**
     * 返回服务唯一id
     * @return hash(服务唯一id)
     */
    public static Integer serviceId(String group, String service, String version) {
        return serviceId(serviceKey(group, service, version));
    }

    /**
     * 返回服务唯一id
     * @param serviceKey 服务gsv
     * @return hash(服务唯一id)
     */
    public static Integer serviceId(String serviceKey) {
        return serviceKey.hashCode();
    }
}
