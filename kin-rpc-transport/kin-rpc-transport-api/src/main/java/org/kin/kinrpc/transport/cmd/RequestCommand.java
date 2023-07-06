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
    private long timeout;

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
    public void serializePayload(ByteBuf out) {
        /*
         * 变长long(1-9): request timeout
         */
        VarIntUtils.writeRawVarInt64(out, timeout);
    }

    @Override
    public void deserialize0(ByteBuf in) {
        timeout = VarIntUtils.readRawVarInt64(in);
    }

    /**
     * process request command时触发
     */
    public void onProcess() {
        execTime = System.currentTimeMillis();
    }

    /**
     * 判断process request command是否超时
     * 一般server端处理结束才会调用
     */
    public boolean isTimeout() {
        return execTime > 0 && System.currentTimeMillis() > timeout;
    }

    //setter && getter
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
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
