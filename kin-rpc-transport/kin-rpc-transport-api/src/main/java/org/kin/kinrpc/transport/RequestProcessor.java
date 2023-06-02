package org.kin.kinrpc.transport;

/**
 * user defined request processor
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface RequestProcessor<R> {
    /** 内置, 定义rpc request processor interest */
    String RPC_REQUEST_INTEREST = "$rpc";

    /**
     * process user defined request
     * @param requestContext   request context
     * @param request   user defined request
     */
    void process(RequestContext requestContext, R request);

    /**
     * 用于寻找唯一的{@link org.kin.kinrpc.transport.RequestProcessor}实例
     * 往往是全限定类名
     * @return  interest
     */
    String interest();

    /**
     * 返回user defined request process executor selector
     * @return executor selector
     */
    ExecutorSelector getExecutorSelector();
}
