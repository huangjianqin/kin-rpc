package org.kin.kinrpc.rpc.invoker;

import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.cluster.Cluster;
import org.kin.kinrpc.rpc.cluster.ClusterConstants;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ClusterInvoker implements InvocationHandler, AsyncInvoker {
    private static final Logger log = LoggerFactory.getLogger("cluster");
    private static final ThreadManager threads = ThreadManager.forkJoinPoolThreadManager();

    private Cluster cluster;

    public ClusterInvoker(Cluster cluster) {
        this.cluster = cluster;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke method '" + method.getName() + "'");
        return invoke(method.getName(), args);
    }

    public Object invoke(String methodName, Object... params) throws Throwable {
        int tryTimes = 0;
        while (tryTimes < ClusterConstants.RETRY_TIMES) {
            ReferenceInvoker invoker = cluster.get();
            if (invoker != null) {
                RPCResponse rpcResponse = (RPCResponse) invoker.invoke(methodName, params);
                switch (rpcResponse.getState()) {
                    case SUCCESS:
                        return rpcResponse.getResult();
                    case RETRY:
                        tryTimes++;
                        break;
                    case ERROR:
                        throw new RuntimeException(rpcResponse.getInfo());
                }
            }
        }
        //超过重试次数, 抛弃异常
        throw new RuntimeException("invoke get unvalid response more than " + ClusterConstants.RETRY_TIMES + " times");
    }

    public Future invokerAsync(String methodName, Object... params) throws Throwable {
        Callable callable = () -> {
            try {
                int tryTimes = 0;
                while (tryTimes < ClusterConstants.RETRY_TIMES) {
                    ReferenceInvoker invoker = cluster.get();
                    if (invoker != null) {
                        Future<RPCResponse> future = invoker.invokerAsync(methodName, params);
                        RPCResponse rpcResponse = future.get();
                        switch (rpcResponse.getState()) {
                            case SUCCESS:
                                return rpcResponse.getResult();
                            case RETRY:
                                tryTimes++;
                                break;
                            case ERROR:
                                throw new RuntimeException(rpcResponse.getInfo());
                        }
                    }
                }
            } catch (Throwable throwable) {
                ExceptionUtils.log(throwable);
            }

            //超过重试次数, 抛弃异常
            throw new RuntimeException("invoke get unvalid response more than " + ClusterConstants.RETRY_TIMES + " times");
        };

        return threads.submit(callable);
    }

    public Cluster getCluster() {
        return cluster;
    }
}
