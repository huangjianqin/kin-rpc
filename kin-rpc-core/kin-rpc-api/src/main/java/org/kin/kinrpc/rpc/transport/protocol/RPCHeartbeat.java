package org.kin.kinrpc.rpc.transport.protocol;

import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.kin.transport.netty.core.protocol.Protocol;
import org.kin.transport.netty.core.protocol.domain.Request;
import org.kin.transport.netty.core.protocol.domain.Response;

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
