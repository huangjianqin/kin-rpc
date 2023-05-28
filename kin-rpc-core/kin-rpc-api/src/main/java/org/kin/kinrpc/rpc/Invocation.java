package org.kin.kinrpc.rpc;

/**
 * {@link Invoker#invoke(Invocation)}参数
 * 包含invoke相关元数据
 * todo 后续包含方法元数据
 * @author huangjianqin
 * @date 2023/2/26
 */
public class Invocation {
    /** 方法名 */
    private final String methodName;
    /** 方法参数实例 */
    private final Object[] params;

    public Invocation(String methodName, Object[] params) {
        this.methodName = methodName;
        this.params = params;
    }

    //getter
    public String getMethodName() {
        return methodName;
    }

    public Object[] getParams() {
        return params;
    }
}
