package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RemotingCodec;

/**
 * @author huangjianqin
 * @date 2023/6/4
 */
public abstract class AbsRemotingServer implements RemotingServer {
    /** remoting codec */
    protected final RemotingCodec codec = new RemotingCodec();
    /** remoting processor */
    protected final RemotingProcessor remotingProcessor = new RemotingProcessor(codec);

    @Override
    public final void registerRequestProcessor(RequestProcessor<?> processor) {
        remotingProcessor.getRequestProcessorManager().register(processor);
    }
}
