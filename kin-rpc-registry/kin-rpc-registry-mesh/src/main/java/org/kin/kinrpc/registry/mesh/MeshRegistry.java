package org.kin.kinrpc.registry.mesh;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.DefaultServiceInstance;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.ServiceMetadataConstants;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.RegistryManager;
import org.kin.kinrpc.registry.ServiceInstanceChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于Sidecar(Proxy) Mesh, 传统基于Envoy Sidecar代理方式, 管理网络流量. 支持与istio配合使用
 * 1. provider不需要配置注册中心直接暴露
 * 2. consumer, 配置providerBy, consumer deployment yaml配置指定环境变量, 因为istio本身支持路由和负载均衡, 所以consumer没必要开启cluster, 可以将cluster设置为failfast
 *
 * @author huangjianqin
 * @date 2023/8/17
 */
public class MeshRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(MeshRegistry.class);

    /** 配置的service instance信息 */
    private final List<ServiceInstance> serviceInstances;
    private boolean terminated;

    public MeshRegistry(RegistryConfig config) {
        super(config);

        List<String> addressList = config.getAddressList();
        List<ServiceInstance> serviceInstances = new ArrayList<>(addressList.size());
        for (String address : addressList) {
            serviceInstances.add(RegistryManager.parseUrl(address));
        }
        this.serviceInstances = serviceInstances;
    }

    @Override
    public void init() {
        //do nothing
    }

    @Override
    public void register(ServiceConfig<?> serviceConfig) {
        log.warn("does not need to config registry when use mesh, please check config");
    }

    @Override
    public void unregister(ServiceConfig<?> serviceConfig) {
        log.warn("does not need to config registry when use mesh, please check config");
    }

    @Override
    public void subscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        if (isTerminated()) {
            throw new IllegalStateException(String.format("%s has been terminated", getClass().getSimpleName()));
        }

        String service = config.getService();
        if (CollectionUtils.isNonEmpty(serviceInstances)) {
            //配置直连url, 直接使用
            Set<ServiceInstance> matchedServiceInstances = serviceInstances.stream()
                    .filter(si -> si.service().equals(service))
                    .collect(Collectors.toSet());

            if (log.isDebugEnabled()) {
                log.debug("{} subscribe service '{}' and find instances, {}", getClass().getSimpleName(), service, matchedServiceInstances);
            }

            listener.onServiceInstanceChanged(matchedServiceInstances);
        } else {
            //根据环境变量构造service instance
            //获取环境变量
            String provideBy = config.getProvideBy();
            String protocol = SysUtils.getSysProperty(MeshConstants.PROTOCOL_KEY, MeshConstants.DEFAULT_PROTOCOL);
            String podNamespace = SysUtils.getSysProperty(MeshConstants.POD_NAMESPACE_KEY, MeshConstants.DEFAULT_POD_NAMESPACE);
            String clusterDomain = SysUtils.getSysProperty(MeshConstants.CLUSTER_DOMAIN_KEY, MeshConstants.DEFAULT_CLUSTER_DOMAIN);
            int port = SysUtils.getIntSysProperty(MeshConstants.PORT_KEY, MeshConstants.DEFAULT_PORT);
            String token = SysUtils.getSysProperty(MeshConstants.TOKEN_KEY);

            //构造service instance
            //{MESH_PROTOCOL}://{providedBy}.{MESH_POD_NAMESPACE}.svc.{MESH_CLUSTER_DOMAIN}:{MESH_PORT}?token={MESH_TOKEN}
            Map<String, String> metadataMap = new HashMap<>(4);
            metadataMap.put(ServiceMetadataConstants.SCHEMA_KEY, protocol);
            if (StringUtils.isNotBlank(token)) {
                metadataMap.put(ServiceMetadataConstants.TOKEN_KEY, token);
            }
            String host = String.join(".", provideBy, podNamespace, "svc", clusterDomain);
            DefaultApplicationInstance.Builder appInstanceBuilder = DefaultApplicationInstance.create();
            appInstanceBuilder.host(host)
                    .port(port)
                    .scheme(protocol);

            Set<ServiceInstance> meshServiceInstances = Collections.singleton(
                    new DefaultServiceInstance(config.getService(), host, port, metadataMap));

            if (log.isDebugEnabled()) {
                log.debug("{} subscribe service '{}' and find instances, {}", getClass().getSimpleName(), service, meshServiceInstances);
            }

            listener.onServiceInstanceChanged(meshServiceInstances);
        }
    }

    @Override
    public void unsubscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        //do nothing
    }

    @Override
    public void destroy() {
        if (isTerminated()) {
            return;
        }

        terminated = true;
    }

    //getter
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public String toString() {
        return "MeshRegistry{" +
                "serviceInstances=" + serviceInstances +
                ", terminated=" + terminated +
                "} " + super.toString();
    }
}
