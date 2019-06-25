package org.kin.kinrpc.registry;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public class RegistryConstants {
    private RegistryConstants(){

    }

    public static final String REGISTRY_ROOT = "/kinrpc";
    public static final String REGISTRY_PAHT_SEPARATOR = "/";

    public static String getPath(String serviceName) {
        return RegistryConstants.REGISTRY_ROOT + RegistryConstants.REGISTRY_PAHT_SEPARATOR + serviceName;
    }

    public static String getPath(String serviceName, String address) {
        return RegistryConstants.REGISTRY_ROOT + RegistryConstants.REGISTRY_PAHT_SEPARATOR + serviceName + RegistryConstants.REGISTRY_PAHT_SEPARATOR + address;
    }
}
