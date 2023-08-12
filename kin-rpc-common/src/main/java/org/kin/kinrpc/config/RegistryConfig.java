package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册中心配置
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public class RegistryConfig extends SharableConfig<RegistryConfig> {
    /** 默认注册中心地址分隔符 */
    public static final String ADDRESS_SEPARATOR = ",";
    /** 默认id生成 */
    private static final AtomicInteger DEFAULT_ID_GEN = new AtomicInteger();
    /** 默认id前缀 */
    private static final String DEFAULT_ID_PREFIX = "$" + RegistryConfig.class.getSimpleName() + "-";

    /**
     * 注册中心alias, 默认是{@link #type} + '#' + {@link #address}
     * 适用于复用已初始化的注册中心
     */
    private String name;
    /** 注册中心类型 */
    private String type;
    /** 注册中心的地址, 如果有多个, 用逗号分隔 */
    private String address;
    /** 应用组 */
    private String group;

    //--------------------------------------------------------------------多注册中心
    /** 注册中心优先级 */
    private Boolean preferred;
    /** 同中心优化, 优先选择具有相同区域的注册中心 */
    private String zone;
    /** 注册中心权重 */
    private Integer weight;

    /**
     * 复用{@link RegistryConfig}实例
     *
     * @param id registry config id
     * @return {@link RegistryConfig}实例
     */
    public static RegistryConfig fromId(String id) {
        return new RegistryConfig().id(id);
    }

    public static RegistryConfig create(String type) {
        return new RegistryConfig().type(type);
    }

    public static RegistryConfig create(String type, String address) {
        return create(type).address(address);
    }

    private static RegistryConfig create(RegistryType type) {
        return create(type.getName());
    }

    private static RegistryConfig create(RegistryType type, String address) {
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
    public void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(type), "registry type must be not blank");
        check(StringUtils.isNotBlank(address), "registry address must be not blank");
        check(StringUtils.isNotBlank(group), "registry group must be not blank");
    }

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (Objects.isNull(group)) {
            group = DefaultConfig.DEFAULT_GROUP;
        }

        if (Objects.isNull(preferred)) {
            preferred = DefaultConfig.DEFAULT_REGISTRY_PREFERRED;
        }

        if (Objects.isNull(zone)) {
            zone = DefaultConfig.DEFAULT_REGISTRY_ZONE;
        }

        if (Objects.isNull(weight)) {
            weight = DefaultConfig.DEFAULT_REGISTRY_WEIGHT;
        }
    }

    /**
     * 将注册中心地址默认分隔符替换为{@code separator}
     *
     * @param separator 用户自定义分隔符
     * @return 替换后的注册中心地址
     */
    public String replaceSeparator(String separator) {
        return address.replace(ADDRESS_SEPARATOR, separator);
    }

    public List<String> getAddressList() {
        return Arrays.asList(address.split(ADDRESS_SEPARATOR));
    }

    @Override
    protected String genDefaultId() {
        return DEFAULT_ID_PREFIX + DEFAULT_ID_GEN.incrementAndGet();
    }

    //setter && getter
    public String getName() {
        return name;
    }

    public RegistryConfig name(String name) {
        this.name = name;
        return this;
    }

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

    public String getGroup() {
        return group;
    }

    public RegistryConfig group(String group) {
        this.group = group;
        return this;
    }

    public boolean isPreferred() {
        return Objects.nonNull(preferred) ? preferred : false;
    }

    public RegistryConfig preferred(Boolean preferred) {
        this.preferred = preferred;
        return this;
    }

    public RegistryConfig preferred() {
        return preferred(true);
    }

    public String getZone() {
        return zone;
    }

    public RegistryConfig zone(String zone) {
        this.zone = zone;
        return this;
    }

    public Integer getWeight() {
        return weight;
    }

    public RegistryConfig weight(Integer weight) {
        this.weight = weight;
        return this;
    }

    //----------------------
    public RegistryConfig setName(String name) {
        this.name = name;
        return this;
    }

    public RegistryConfig setType(String type) {
        this.type = type;
        return this;
    }

    public RegistryConfig setAddress(String address) {
        this.address = address;
        return this;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Boolean getPreferred() {
        return preferred;
    }

    public void setPreferred(Boolean preferred) {
        this.preferred = preferred;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "RegistryConfig{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", address='" + address + '\'' +
                ", group='" + group + '\'' +
                ", preferred=" + preferred +
                ", zone='" + zone + '\'' +
                ", weight=" + weight +
                ", " + super.toString() +
                '}';
    }
}
