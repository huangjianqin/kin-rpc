package org.kin.kinrpc.rpc.common.config;

/**
 * 注册中心配置
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public class RegistryConfig extends AbstractConfig {
    /** 默认注册中心地址分隔符 */
    private static final String DEFAULT_ADDRESS_SEPARATOR = ";";
    /** 注册中心类型 */
    private String type;
    /** 注册中心的地址, 如果有多个, 用分号分隔 */
    private String address;

    public static RegistryConfig create(String type){
        return new RegistryConfig().type(type);
    }

    public static RegistryConfig create(String type, String address){
        return create(type).address(address);
    }

    public static RegistryConfig create(RegistryType type){
        return create(type.getName());
    }

    public static RegistryConfig create(RegistryType type, String address){
        return create(type.getName(), address);
    }

    private RegistryConfig() {
    }

    /**
     * 将注册中心地址默认分隔符替换为{@code separator}
     * @param separator 用户自定义分隔符
     * @return  替换后的注册中心地址
     */
    public String replaceSeparator(String separator){
        return address.replace(DEFAULT_ADDRESS_SEPARATOR, separator);
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
