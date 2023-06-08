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

    /** listen host */
    protected final String host;
    /** listen port */
    protected final int port;

    protected AbsRemotingServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <RS extends RemotingServer> RS registerRequestProcessor(RequestProcessor<?> processor) {
        remotingProcessor.getRequestProcessorManager().register(processor);
        return (RS) this;
    }

    @Override
    public final String host() {
        return host ;
    }

    @Override
    public final int port() {
        return port;
    }
}
