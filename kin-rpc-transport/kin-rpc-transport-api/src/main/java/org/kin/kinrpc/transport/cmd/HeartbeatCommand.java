package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;

/**
 * heartbeat command
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.HEARTBEAT)
public final class HeartbeatCommand extends RemotingCommand {
    private static final long serialVersionUID = -4897142987926394746L;

    public HeartbeatCommand() {
    }

    public HeartbeatCommand(short version, long id) {
        super(CommandCodes.HEARTBEAT, version, id);
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        //do nothing
    }

    @Override
    public void deserialize0(ByteBuf payload) {
        //do nothing
    }
}
