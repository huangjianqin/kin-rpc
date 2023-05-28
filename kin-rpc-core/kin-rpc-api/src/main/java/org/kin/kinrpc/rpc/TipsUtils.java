package org.kin.kinrpc.rpc;

import java.util.StringJoiner;

/**
 * 提示工具类
 * @author huangjianqin
 * @date 2020/11/4
 */
public class TipsUtils {
    /**
     * @return 服务名+方法名+(参数1,参数2..)
     */
    public static String genInvokeMessage(String serviceName, String methodName, Object[] params) {
        StringBuilder sb = new StringBuilder();
        StringJoiner sj = new StringJoiner(", ");
        sb.append("(");
        for (Object param : params) {
            sj.add(param.getClass().getName());
        }
        sb.append(sj);
        sb.append(")");
        return serviceName.concat("$").concat(methodName).concat(sb.toString());
    }
}
