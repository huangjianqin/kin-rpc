package org.kin.kinrpc;

import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public class DefaultApplicationInstance implements ApplicationInstance {
    /** 所属组 */
    private final String group;
    /** 应用实例host */
    private final String host;
    /** 应用实例端口 */
    private final int port;
    /** 应用实例schema */
    private final String scheme;
    /** 应用元数据 */
    private final Map<String, String> metadata;

    public DefaultApplicationInstance(String group,
                                      String host,
                                      int port,
                                      Map<String, String> metadata) {
        this.group = group;
        this.host = host;
        this.port = port;
        this.metadata = metadata;
        this.scheme = metadata(ServiceMetadataConstants.SCHEMA_KEY);
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public String scheme() {
        return scheme;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultApplicationInstance that = (DefaultApplicationInstance) o;
        return port == that.port && Objects.equals(group, that.group) && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, host, port);
    }
}
