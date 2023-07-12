package org.kin.kinrpc.protocol.rsocket;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.protocol.AbstractProtocol;

import static org.kin.kinrpc.protocol.rsocket.RSocketProtocol.NAME;

/**
 * @author huangjianqin
 * @date 2021/1/30
 */
@Extension(NAME)
public class RSocketProtocol extends AbstractProtocol {
    /** 协议名 */
    public static final String NAME = "rsocket";

    @Override
    protected String name() {
        return NAME;
    }
}
