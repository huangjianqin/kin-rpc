package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;

/**
 * 共享配置, 支持service或者reference配置中通过{@link #id}来引用配置
 * 仅在使用{@link org.kin.kinrpc.bootstrap.KinRpcBootstrap}时生效
 *
 * @author huangjianqin
 * @date 2023/7/10
 * @see org.kin.kinrpc.bootstrap.KinRpcBootstrap
 */
public abstract class SharableConfig<SC extends SharableConfig<SC>> extends AttachableConfig<SC> {
    /**
     * 配置唯一id, 用于引用全局配置
     * 如果不需要引用配置, 则不配置即可
     */
    private String id;

    /**
     * 生成默认配置id
     *
     * @return 默认配置id
     */
    protected abstract String genDefaultId();

    //setter && getter
    public final String getId() {
        if (StringUtils.isBlank(id)) {
            id = genDefaultId();
        }
        return id;
    }

    public final SC id(String id) {
        this.id = id;
        return castThis();
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "id='" + id + '\'' +
                ", " + super.toString();
    }
}
