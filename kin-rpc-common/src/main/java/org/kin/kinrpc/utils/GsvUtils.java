package org.kin.kinrpc.utils;

import io.micrometer.core.instrument.util.StringUtils;
import org.kin.framework.utils.MurmurHash3;

/**
 * 服务唯一标识工具类
 *
 * @author huangjianqin
 * @date 2023/2/25
 */
public final class GsvUtils {
    private GsvUtils() {
    }

    /**
     * group:serviceName:version
     *
     * @return 服务唯一标识
     */
    public static String service(String group, String serviceName, String version) {
        int length = serviceName == null ? 0 : serviceName.length();
        length += group == null ? 0 : group.length();
        length += version == null ? 0 : version.length();
        length += 2;
        StringBuilder buf = new StringBuilder(length);
        if (StringUtils.isNotEmpty(group)) {
            buf.append(group).append(':');
        }
        buf.append(serviceName);
        if (StringUtils.isNotEmpty(version)) {
            buf.append(':').append(version);
        }
        return buf.toString().intern();
    }

    /**
     * 返回服务唯一id
     *
     * @return 服务唯一id, hash(服务唯一标识)
     */
    public static int serviceId(String group, String serviceName, String version) {
        return serviceId(service(group, serviceName, version));
    }

    /**
     * 返回服务唯一id
     *
     * @param service 服务唯一标识
     * @return 服务唯一id, hash(服务唯一标识)
     */
    public static int serviceId(String service) {
        return MurmurHash3.hash32(service);
    }
}
