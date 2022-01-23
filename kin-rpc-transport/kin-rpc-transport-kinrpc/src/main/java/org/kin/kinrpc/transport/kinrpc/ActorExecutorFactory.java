package org.kin.kinrpc.transport.kinrpc;

import io.netty.channel.Channel;
import org.kin.framework.concurrent.EventLoop;
import org.kin.framework.concurrent.EventLoopGroup;
import org.kin.framework.concurrent.MultiThreadEventLoopGroup;
import org.kin.framework.concurrent.SingleThreadEventLoop;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 类actor风格处理rpc请求, 指定服务的所有请求在同一线程处理
 *
 * @author huangjianqin
 * @date 2021/4/20
 * @see SingleThreadEventLoop
 */
final class ActorExecutorFactory extends AbstractExecutorFactory {
    private final EventLoopGroup<SingleThreadEventLoop> eventLoopGroup;
    /** key -> service key, value -> 该service对应的EventLoop */
    private final ConcurrentHashMap<String, EventLoop<SingleThreadEventLoop>> service2EventLoop = new ConcurrentHashMap<>();

    public ActorExecutorFactory(Url url, int port) {
        super(url, port);
        int cores = url.getIntParam(Constants.CORES_KEY, Constants.CORES);

        eventLoopGroup = new MultiThreadEventLoopGroup(cores, "rpc-provider-" + port);
    }

    @Override
    public Executor executor(RpcRequest rpcRequest, Channel channel) {
        String serviceKey = rpcRequest.getServiceKey();
        return service2EventLoop.computeIfAbsent(serviceKey, k -> eventLoopGroup.next());
    }

    @Override
    public void close() {
        service2EventLoop.clear();
        eventLoopGroup.shutdown();
    }
}
