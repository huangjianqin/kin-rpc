package org.kin.kinrpc.rpc.common;

/**
 * url query参数, 可共享, 即可以存储在注册中心
 * {@link Url}query参数
 * todo ssl
 * @author huangjianqin
 * @date 2023/2/15
 */
public enum UrlParamKey {
    APP("app", "应用名"),
    GROUP("group", "服务组"),
    SERVICE("service", "服务名"),
    VERSION("version", "服务版本", "1.0.0.0"),
    EXPORT("export","基于注册中心运行时, 暴露的服务URL"),
    REFER("refer","基于注册中心运行时, 引用的服务URL"),
    SERIALIZATION("serialization","序列化类型"),
    INTERFACE("interface", "服务接口"),
    ;
    private final String name;
    private final String desc;
    private Object defaultValue;

    UrlParamKey(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
    
    UrlParamKey(String name, String desc, Object defaultValue) {
        this.name = name;
        this.desc = desc;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
