package org.kin.kinrpc.registry.common;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public interface RegistryConstants {

    String REGISTRY_ROOT = "kinrpc";
    String REGISTRY_PAHT_SEPARATOR = "/";

    /** 注册中心root path */
    static String root() {
        return RegistryConstants.REGISTRY_PAHT_SEPARATOR + RegistryConstants.REGISTRY_ROOT;
    }

    /** 注册中心, 指定serviceName的path */
    static String getPath(String serviceName) {
        return root() + RegistryConstants.REGISTRY_PAHT_SEPARATOR + serviceName;
    }

    /** 注册中心, 指定serviceName和address的path */
    static String getPath(String serviceName, String address) {
        return getPath(serviceName) + RegistryConstants.REGISTRY_PAHT_SEPARATOR + address;
    }
}
