package org.kin.kinrpc.rpc.common;

/**
 * url的一些属性, 不共享, 仅本应用使用
 * @author huangjianqin
 * @date 2023/2/19
 */
public enum AttributeKey {
    INVOCATION("invocation", "rpc call信息"),
    EXECUTOR("executor", "服务处理线程池")
    ;
    private final String name;
    private final String desc;

    AttributeKey(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }
}
