package org.kin.kinrpc.registry;


import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Directory {
    /**
     * 获取所有发现的invokers
     *
     * @return 所有发现的invokers
     */
    List<ReferenceInvoker> list();

    /**
     * 发现并连接invokers
     * @param addresses 发现并连接invokers的address
     */
    void discover(List<String> addresses);

    /**
     * 获取服务名
     * @return 服务名
     */
    String getServiceName();

    /**
     * 销毁
     */
    void destroy();
}
