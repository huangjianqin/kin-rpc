package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 注册中心配置
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public class RegistryConfig extends AttachableConfig {
    /** 默认注册中心地址分隔符 */
    private static final String DEFAULT_ADDRESS_SEPARATOR = ";";
    /** 注册中心类型 */
    private String type;
    /** 注册中心的地址, 如果有多个, 用分号分隔 */
    private String address;

    public static RegistryConfig create(String type) {
        return new RegistryConfig().type(type);
    }

    public static RegistryConfig create(String type, String address){
        return create(type).address(address);
    }

    public static RegistryConfig create(RegistryType type) {
        return create(type.getName());
    }

    public static RegistryConfig create(RegistryType type, String address) {
        return create(type.getName(), address);
    }

    public static RegistryConfig direct(String address) {
        return create(RegistryType.DIRECT, address);
    }

    public static RegistryConfig zk(String address) {
        return create(RegistryType.ZOOKEEPER, address);
    }

    public static RegistryConfig nacos(String address) {
        return create(RegistryType.NACOS, address);
    }

    public static RegistryConfig etcd(String address) {
        return create(RegistryType.ETCD, address);
    }

    public static RegistryConfig consul(String address) {
        return create(RegistryType.CONSUL, address);
    }

    public static RegistryConfig k8s(String address) {
        return create(RegistryType.K8S, address);
    }

    private RegistryConfig() {
    }

    @Override
    protected void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(type), "registry type must be not blank");
        check(StringUtils.isNotBlank(address), "registry address must be not blank");
    }

    /**
     * 将注册中心地址默认分隔符替换为{@code separator}
     *
     * @param separator 用户自定义分隔符
     * @return 替换后的注册中心地址
     */
    public String replaceSeparator(String separator) {
        return address.replace(DEFAULT_ADDRESS_SEPARATOR, separator);
    }

    public List<String> getAddressList() {
        return Arrays.asList(address.split(DEFAULT_ADDRESS_SEPARATOR));
    }

    //setter && getter
    public String getType() {
        return type;
    }

    public RegistryConfig type(String type) {
        this.type = type;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public RegistryConfig address(String address) {
        this.address = address;
        return this;
    }
}
