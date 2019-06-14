package org.kin.kinrpc.rpc.transport;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.transport.protocol.domain.AbstractProtocol;
import org.kin.kinrpc.transport.protocol.domain.Request;
import org.kin.kinrpc.transport.protocol.domain.Response;

/**
 * Created by huangjianqin on 2019/6/14.
 * 相对server而言
 */
public class RPCRequestProtocol extends AbstractProtocol {
    private byte[] reqContent;

    public RPCRequestProtocol(int protocolId) {
        super(protocolId);
        Preconditions.checkArgument(protocolId == RPCConstants.RPC_REQUEST_PROTOCOL_ID);
    }

    public RPCRequestProtocol(byte[] reqContent) {
        super(RPCConstants.RPC_REQUEST_PROTOCOL_ID);
    }

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
