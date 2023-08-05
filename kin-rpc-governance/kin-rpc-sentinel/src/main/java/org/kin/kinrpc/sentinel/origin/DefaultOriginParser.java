package org.kin.kinrpc.sentinel.origin;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.constants.InvocationConstants;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
public class DefaultOriginParser implements OriginParser {
    @Override
    public String parse(Invocation invocation) {
        String appName = invocation.attachment(InvocationConstants.APPLICATION_KEY);
        if (StringUtils.isBlank(appName)) {
            throw new IllegalArgumentException("bad invocation");
        }
        return appName;
    }
}
