package org.kin.kinrpc.protocol.kinrpc;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.protocol.AbstractProtocol;

import static org.kin.kinrpc.protocol.kinrpc.KinRpcProtocol.NAME;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
@Extension(NAME)
public final class KinRpcProtocol extends AbstractProtocol {
    /** 协议名 */
    public static final String NAME = "kinrpc";

    @Override
    protected String name() {
        return NAME;
    }
}
