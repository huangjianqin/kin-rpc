package org.kin.kinrpc.rpc;

import java.util.StringJoiner;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
public class KinRpcUtils {
    /**
     * @return 服务名+方法名+(参数1,参数2..)
     */
    public static String generateInvokeMsg(String serviceName, String methodName, Object[] params) {
        StringBuilder sb = new StringBuilder();
        StringJoiner sj = new StringJoiner(", ");
        sb.append("(");
        for (Object param : params) {
            sj.add(param.getClass().getName());
        }
        sb.append(sj.toString());
        sb.append(")");
        return serviceName.concat("$").concat(methodName).concat(sb.toString());
    }
}
