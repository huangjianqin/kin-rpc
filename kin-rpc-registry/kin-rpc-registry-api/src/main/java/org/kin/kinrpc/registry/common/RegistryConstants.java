package org.kin.kinrpc.registry.common;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public class RegistryConstants {
    private RegistryConstants() {

    }

    public static final String REGISTRY_ROOT = "kinrpc";
    public static final String REGISTRY_PAHT_SEPARATOR = "/";

    public static String root() {
        return RegistryConstants.REGISTRY_PAHT_SEPARATOR + RegistryConstants.REGISTRY_ROOT;
    }

    public static String getPath(String serviceName) {
        return root() + RegistryConstants.REGISTRY_PAHT_SEPARATOR + serviceName;
    }

    public static String getPath(String serviceName, String address) {
        return getPath(serviceName) + RegistryConstants.REGISTRY_PAHT_SEPARATOR + address;
    }
}
