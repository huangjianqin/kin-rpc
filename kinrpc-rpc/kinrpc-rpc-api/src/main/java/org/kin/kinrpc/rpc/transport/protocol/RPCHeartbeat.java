package org.kin.kinrpc.rpc.transport.protocol;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.transport.domain.Request;
import org.kin.kinrpc.transport.domain.Response;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class RPCHeartbeat extends AbstractProtocol {
    private String ip;
    private String content;

    public RPCHeartbeat(int protocolId) {
        super(protocolId);
        Preconditions.checkArgument(protocolId == RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID);
    }

    public RPCHeartbeat(String ip, String content) {
        super(RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID);
        this.ip = ip;
        this.content = content;
    }

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
