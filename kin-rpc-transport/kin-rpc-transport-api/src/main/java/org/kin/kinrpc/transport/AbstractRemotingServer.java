package org.kin.kinrpc.transport;

import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.executor.DefaultManagedExecutor;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.cmd.RemotingCodec;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/6/4
 */
public abstract class AbstractRemotingServer implements RemotingServer {
    /** remoting codec */
    protected final RemotingCodec codec = new RemotingCodec();
    /** remoting processor */
    protected final RemotingProcessor remotingProcessor;

    /** listen host */
    protected final String host;
    /** listen port */
    protected final int port;

    protected AbstractRemotingServer(String host, int port, @Nullable ManagedExecutor executor) {
        Preconditions.checkArgument(port > 0, "server port must be greater than 0");
        if (StringUtils.isBlank(host)) {
            host = NetUtils.getLocalhostIp();
        }
        this.host = host;
        this.port = port;
        if (Objects.isNull(executor)) {
            String executorName = NetUtils.getIpPort(host, port) + "-command-processor";
            executor = new DefaultManagedExecutor(
                    ThreadPoolUtils.newThreadPool(executorName, true,
                            SysUtils.CPU_NUM, SysUtils.DOUBLE_CPU,
                            60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(1024), new SimpleThreadFactory(executorName),
                            new ThreadPoolExecutor.AbortPolicy()));
        }
        this.remotingProcessor = new RemotingProcessor(codec, executor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <RS extends RemotingServer> RS registerRequestProcessor(RequestProcessor<?> processor) {
        remotingProcessor.getRequestProcessorManager().register(processor);
        return (RS) this;
    }

    @Override
    public final String host() {
        return host;
    }

    @Override
    public final int port() {
        return port;
    }
}
