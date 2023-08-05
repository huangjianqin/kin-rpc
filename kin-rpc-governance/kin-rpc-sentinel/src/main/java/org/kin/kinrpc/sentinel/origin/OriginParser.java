package org.kin.kinrpc.sentinel.origin;

import com.alibaba.csp.sentinel.context.Context;
import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Invocation;

/**
 * Customized origin parser for kinrpc provider filter {@link Context#getOrigin()}
 *
 * @author huangjianqin
 * @date 2023/8/5
 */
@SPI(alias = "originParser")
public interface OriginParser {
    /**
     * parses the origin (caller) from kinrpc invocation.
     *
     * @param invocation kinrpc invocation
     * @return the parsed origin
     */
    String parse(Invocation invocation);
}
