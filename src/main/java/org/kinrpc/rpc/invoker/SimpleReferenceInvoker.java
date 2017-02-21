package org.kinrpc.rpc.invoker;

import io.netty.channel.EventLoopGroup;
import org.apache.log4j.Logger;
import org.kinrpc.common.RPCRequestIdGenerator;
import org.kinrpc.remoting.transport.bootstrap.ReferenceConnection;
import org.kinrpc.rpc.future.RPCFuture;
import org.kinrpc.rpc.protol.RPCRequest;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class SimpleReferenceInvoker extends ReferenceInvoker {
    private static final Logger log = Logger.getLogger(SimpleReferenceInvoker.class);

    public SimpleReferenceInvoker(Class<?> interfaceClass) {
        super(interfaceClass);
    }

    @Override
    public void init(String host, int port, EventLoopGroup eventLoopGroup) {
        log.info("ReferenceInvoker initing...");
        connection = new ReferenceConnection(new InetSocketAddress(host, port), eventLoopGroup);
        connection.connect();
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
