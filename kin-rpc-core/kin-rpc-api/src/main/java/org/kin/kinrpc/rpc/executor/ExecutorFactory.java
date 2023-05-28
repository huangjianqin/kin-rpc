package org.kin.kinrpc.rpc.executor;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.Executor;

/**
 * 服务方法调用executor
 *
 * @author huangjianqin
 * @date 2021/4/20
 */
@SPI(value = "fix", alias = "executorFactory")
public interface ExecutorFactory {
    /**
     * 返回服务配置的executor
     *
     * @param executorName executor name, 即executor唯一标识
     * @param url          服务url
     * @return 返回服务配置的executor
     */
    Executor executor(String executorName, Url url);
}
