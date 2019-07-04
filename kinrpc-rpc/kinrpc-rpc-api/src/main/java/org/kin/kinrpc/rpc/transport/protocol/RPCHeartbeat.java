package org.kin.kinrpc.rpc.transport.protocol;

import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.transport.protocol.Protocol;
import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.domain.Response;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
@Protocol(id = RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID)
public class RPCHeartbeat extends AbstractProtocol {
    private String ip;
    private String content;

    @Override
    public void read(Request request) {
        ip = request.readString();
        content = request.readString();
    }

    @Override
    public void write(Response response) {
        response.writeString(ip);
        response.writeString(content);
    }

    //setter && getter
    public String getIp() {
        return ip;
    }

    public String getContent() {
        return content;
    }
}
