package org.kin.kinrpc.registry;

import org.kin.framework.utils.MD5;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ServiceMetadata;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
public class ApplicationMetadata {
    /** 分隔符 */
    private static final String SEPARATOR = "/";

    /** 应用名 */
    private final String appName;
    /** 应用server配置 */
    private final ServerConfig serverConfig;
    /** 元数据版本, 全局唯一 */
    private String revision;
    /** key -> 服务唯一标识, value -> 服务元数据 */
    private final Map<String, ServiceMetadata> serviceMetadataMap = new TreeMap<>();
    /** 标识元数据是否有更新 */
    private volatile boolean update;
    /**
     * 已注册的服务元数据
     * key -> 服务唯一标识, value -> 服务元数据
     */
    private volatile Map<String, ServiceMetadata> registeredServiceMetadataMap = Collections.emptyMap();

    public ApplicationMetadata(String appName, ServerConfig serverConfig) {
        this.appName = appName;
        this.serverConfig = serverConfig;
    }

    /**
     * 注册暴露服务的元数据
     *
     * @param serviceConfig 服务配置
     */
    public synchronized void register(ServiceConfig<?> serviceConfig) {
        serviceMetadataMap.put(serviceConfig.getService(), serviceConfig.getMetadata());
        update = true;
    }

    /**
     * 注销服务的元数据
     *
     * @param service 服务唯一标识
     */
    public synchronized void unregister(String service) {
        serviceMetadataMap.remove(service);
        update = true;
    }

    /**
     * 计算revision, 如果有变化, 则更新{@link #revision}
     *
     * @return 最新revision
     */
    public synchronized String calOrUpdateRevision() {
        if (StringUtils.isNotBlank(revision) && !update) {
            return revision;
        }

        update = false;

        StringBuilder buffer = new StringBuilder();
        buffer.append(getAddress()).append(SEPARATOR)
                .append(appName).append(SEPARATOR);
        for (String service : serviceMetadataMap.keySet()) {
            buffer.append(service);
        }

        this.revision = MD5.common().digestAsHex(buffer.toString());
        //copy and no change
        this.registeredServiceMetadataMap = Collections.unmodifiableMap(new TreeMap<>(this.serviceMetadataMap));

        return this.revision;
    }

    //getter
    public String getAppName() {
        return appName;
    }

    public String getAddress() {
        return serverConfig.getAddress();
    }

    public String getProtocol() {
        return serverConfig.getProtocol();
    }

    public String getRevision() {
        return revision;
    }

    public Map<String, ServiceMetadata> getRegisteredServiceMetadataMap() {
        return registeredServiceMetadataMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationMetadata)) {
            return false;
        }
        ApplicationMetadata that = (ApplicationMetadata) o;
        return Objects.equals(appName, that.appName) && Objects.equals(serverConfig, that.serverConfig) && Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, serverConfig, revision);
    }
}
