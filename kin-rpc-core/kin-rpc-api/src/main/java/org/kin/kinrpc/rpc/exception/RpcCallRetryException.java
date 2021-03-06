package org.kin.kinrpc.rpc.exception;

import org.kin.framework.utils.StringUtils;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class RpcCallRetryException extends RuntimeException {
    private static final long serialVersionUID = -841758269129755416L;

    public RpcCallRetryException(String respInfo, String serviceKey, String methodName, Object... params) {
        super(respInfo + System.lineSeparator() +
                "need to retry to invoke service '" + serviceKey +
                "', methodName '" + methodName +
                "', params '" + StringUtils.mkString(params) + "'");
    }

    public RpcCallRetryException(Throwable cause) {
        super(cause);
    }
}
