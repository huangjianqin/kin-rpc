package org.kin.kinrpc.config;

/**
 * 统一入口
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class KinRpc {
    private KinRpc() {
    }

    /**
     * 服务暴露配置
     *
     * @param interfaceClass 服务接口
     * @param service        服务实例
     */
    public static <T> ServiceConfig<T> service(Class<T> interfaceClass, T service) {
        return ServiceConfig.create(interfaceClass, service);
    }

    /**
     * 创建服务引用配置
     *
     * @param interfaceClass 服务接口
     */
    public static <T> ReferenceConfig<T> reference(Class<T> interfaceClass) {
        return ReferenceConfig.create(interfaceClass);
    }
}
