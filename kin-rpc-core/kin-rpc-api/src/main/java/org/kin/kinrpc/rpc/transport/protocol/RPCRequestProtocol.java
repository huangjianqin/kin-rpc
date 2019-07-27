package org.kin.kinrpc.rpc.transport.protocol;

import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.transport.netty.domain.Request;
import org.kin.kinrpc.transport.netty.domain.Response;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.netty.protocol.Protocol;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
@Protocol(id = RPCConstants.RPC_REQUEST_PROTOCOL_ID)
public class RPCRequestProtocol extends AbstractProtocol {
    private byte[] reqContent;

    @Override
    public void read(Request request) {
        reqContent = request.readBytes();
    }

    @Override
    public void write(Response response) {
        response.writeBytes(reqContent);
    }

    //setter && getter
    public byte[] getReqContent() {
        return reqContent;
    }

    public void setReqContent(byte[] reqContent) {
        this.reqContent = reqContent;
    }
}
