package org.kin.kinrpc;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public class DefaultApplicationInstance implements ApplicationInstance {
    /** 应用实例host */
    private String host;
    /** 应用实例端口 */
    private int port;
    /** 应用实例schema */
    private String scheme;
    /** 应用服务版本呢 */
    private String revision;
    /** 应用元数据 */
    private Map<String, String> metadata;

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
        if (Objects.isNull(metadata)) {
            return Collections.emptyMap();
        }
        return metadata;
    }

    @Override
    public String scheme() {
        return scheme;
    }

    @Override
    public String revision() {
        return revision;
    }

    //setter && getter
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultApplicationInstance that = (DefaultApplicationInstance) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "DefaultApplicationInstance{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", scheme='" + scheme + '\'' +
                ", revision='" + revision + '\'' +
                ", metadata=" + metadata +
                '}';
    }

    //-------------------------------------------------------------------------------------------------------------------
    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private DefaultApplicationInstance instance = new DefaultApplicationInstance();

        public Builder host(String host) {
            this.instance.setHost(host);
            return this;
        }

        public Builder port(int port) {
            this.instance.setPort(port);
            return this;
        }

        public Builder scheme(String scheme) {
            this.instance.setScheme(scheme);
            return this;
        }

        public Builder revision(String revision) {
            this.instance.setRevision(revision);
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.instance.setMetadata(metadata);
            return this;
        }

        public DefaultApplicationInstance build() {
            return instance;
        }
    }
}
