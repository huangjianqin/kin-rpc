package org.kin.kinrpc.rpc.exception;

import org.kin.framework.utils.StringUtils;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class RpcRetryException extends RuntimeException {
    public RpcRetryException(String respInfo, String serviceName, String methodName, Object... params) {
        super(respInfo + System.lineSeparator() +
                "need to retry to invoke service '" + serviceName +
                "', methodName '" + methodName +
                "', params '" + StringUtils.mkString(params) + "'");
    }
}