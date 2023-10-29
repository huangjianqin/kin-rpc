package org.kin.kinrpc.protocol.rest;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.protocol.AbstractProtocol;

import static org.kin.kinrpc.protocol.rest.RestProtocol.NAME;

/**
 * @author huangjianqin
 * @date 2023/10/29
 */
@Extension(NAME)
public class RestProtocol extends AbstractProtocol {
    /** 协议名 */
    public static final String NAME = "rest";

    @Override
    protected String name() {
        return NAME;
    }
}
