package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import org.kin.transport.netty.utils.VarIntUtils;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public abstract class RequestCommand extends RemotingCommand {
    private static final long serialVersionUID = 6116244388809293027L;

    /** 请求超时时间 */
    private int timeout;

    /** 请求到达时间 */
    private transient long arriveTime = System.currentTimeMillis();
    /** 请求开始处理时间 */
    private transient long execTime = -1;

    protected RequestCommand() {
    }

    protected RequestCommand(short cmdCode, short version, long id, byte serializationCode) {
        super(cmdCode, version, id);
        setSerializationCode(serializationCode);
    }

    @Override
    public void serialize(ByteBuf out) {
        /*
         * 变长int(1-5): request timeout
         */
        VarIntUtils.writeRawVarInt32(out, timeout);
    }

    @Override
    public void deserialize0(ByteBuf payload) {
        timeout = VarIntUtils.readRawVarInt32(payload);
    }

    //setter && getter
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public long getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(long arriveTime) {
        this.arriveTime = arriveTime;
    }

    public long getExecTime() {
        return execTime;
    }

    public void setExecTime(long execTime) {
        this.execTime = execTime;
    }
}
