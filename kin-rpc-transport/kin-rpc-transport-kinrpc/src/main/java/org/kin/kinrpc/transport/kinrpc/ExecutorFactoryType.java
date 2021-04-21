package org.kin.kinrpc.transport.kinrpc;

import org.kin.kinrpc.rpc.common.Url;

/**
 * @author huangjianqin
 * @date 2021/4/20
 */
public enum ExecutorFactoryType {
    /** 类actor风格处理rpc请求, 指定服务的所有请求在同一线程处理 */
    ACTOR() {
        @Override
        public ExecutorFactory create(Url url, int port) {
            return new ActorExecutorFactory(url, port);
        }
    },
    /** 多线程处理rpc请求, 优先创建多个线程处理rpc请求, 如果仍然处理不过来, 则入队 */
    EAGER() {
        @Override
        public ExecutorFactory create(Url url, int port) {
            return new EagerExecutorFactory(url, port);
        }
    },
    /** 在netty io线程处理rpc请求 */
    DIRECT() {
        @Override
        public ExecutorFactory create(Url url, int port) {
            return new DirectExecutorFactory(url, port);
        }
    },
    /** 多线程处理rpc请求, 不会将task入队, 直接reject */
    CACHE() {
        @Override
        public ExecutorFactory create(Url url, int port) {
            return new CacheExecutorFactory(url, port);
        }
    },
    /** 固定线程数处理rpc请求 */
    FIX() {
        @Override
        public ExecutorFactory create(Url url, int port) {
            return new FixExecutorFactory(url, port);
        }
    },
    ;

    private static final ExecutorFactoryType[] values = values();

    /**
     * 根据具体类型创建对应的{@link ExecutorFactory}
     */
    public abstract ExecutorFactory create(Url url, int port);

    public static ExecutorFactoryType getByName(String name) {
        for (ExecutorFactoryType type : values) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("unknown ExecutorFactory type '%s'", name));
    }
}
