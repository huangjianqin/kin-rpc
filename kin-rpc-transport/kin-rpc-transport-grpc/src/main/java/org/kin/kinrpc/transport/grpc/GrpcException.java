package org.kin.kinrpc.transport.grpc;

import org.kin.kinrpc.transport.TransportException;

/**
 * @author huangjianqin
 * @date 2023/6/29
 */
public class GrpcException extends TransportException {
    private static final long serialVersionUID = 3350157848872549625L;

    public GrpcException() {
    }

    public GrpcException(String message) {
        super(message);
    }

    public GrpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrpcException(Throwable cause) {
        super(cause);
    }

    public GrpcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
