package org.kin.kinrpc.registry.kubernetes;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryFactory;

/**
 * @author huangjianqin
 * @date 2023/8/27
 */
@Extension("k8s")
public class KubernetesRegistryFactory implements RegistryFactory {
    @Override
    public Registry create(RegistryConfig config) {
        return new KubernetesRegistry(config);
    }
}
