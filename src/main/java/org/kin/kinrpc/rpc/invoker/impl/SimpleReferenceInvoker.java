package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.kinrpc.remoting.transport.ClientConnection;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.kin.kinrpc.rpc.protocol.RPCRequestIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class SimpleReferenceInvoker extends ReferenceInvoker {
    private static final Logger log = LoggerFactory.getLogger(SimpleReferenceInvoker.class);

    public SimpleReferenceInvoker(Class<?> interfaceClass, ClientConnection connection) {
        super(interfaceClass, connection);
    }

    @Override
    public void init() {
        log.info("ReferenceInvoker initing...");
        this.connection.connect();
    }

    @Override
    public void shutdown() {
        log.info("ReferenceInvoker shutdowning...");
        connection.close();
    }


    public Object invoke(String methodName, Object... params) {
        log.info("invoker method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        RPCFuture future = connection.request(request);
        try {
            return future.get();
        } catch (InterruptedException e) {
            log.info("pending result interrupted");
            e.printStackTrace();
        } catch (ExecutionException e) {
            log.info("pending result execute error");
            e.printStackTrace();
        }

        return null;
    }

    public RPCFuture invokerAsync(String methodName, Object... params) {
        log.info("invokerAsync method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        RPCFuture future = connection.request(request);
        return future;
    }
}
