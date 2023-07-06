package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import org.kin.kinrpc.transport.RequestIdGenerator;
import org.kin.kinrpc.transport.TransportConstants;

/**
 * heartbeat command
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.HEARTBEAT)
public final class HeartbeatCommand extends RemotingCommand {
    private static final long serialVersionUID = -4897142987926394746L;

    public HeartbeatCommand() {
        this(TransportConstants.VERSION, RequestIdGenerator.next());
    }

    public HeartbeatCommand(short version, long id) {
        super(CommandCodes.HEARTBEAT, version, id);
    }

    @Override
    public void serializePayload(ByteBuf byteBuf) {
        //do nothing
    }

    @Override
    public void deserialize0(ByteBuf payload) {
        //do nothing
    }
}
