package org.kin.kinrpc;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public class RpcInvocation implements Invocation {
    /** 服务唯一标识 */
    private final int serviceId;
    /** 服务唯一标识 */
    private final String gsv;
    /** 方法参数实例 */
    private final Object[] params;
    /** 服务方法元数据 */
    private final MethodMetadata methodMetadata;

    public RpcInvocation(int serviceId,
                         String gsv,
                         Object[] params,
                         MethodMetadata methodMetadata) {
        this.serviceId = serviceId;
        this.gsv = gsv;
        this.params = params;
        this.methodMetadata = methodMetadata;
    }

    @Override
    public int getServiceId() {
        return serviceId;
    }

    @Override
    public String getGsv() {
        return gsv;
    }

    @Override
    public String getMethodName() {
        return methodMetadata.getName();
    }

    @Override
    public Object[] getParams() {
        return params;
    }

    @Override
    public boolean isAsyncReturn() {
        return methodMetadata.isAsyncReturn();
    }

    @Override
    public boolean isOneWay() {
        return methodMetadata.isOneWay();
    }

    @Override
    public Class<?> getRealReturnType() {
        return methodMetadata.getRealReturnType();
    }

    @Override
    public Class<?> getReturnType() {
        return methodMetadata.getReturnType();
    }

    //getter
    public MethodMetadata getMethodMetadata() {
        return methodMetadata;
    }
}
