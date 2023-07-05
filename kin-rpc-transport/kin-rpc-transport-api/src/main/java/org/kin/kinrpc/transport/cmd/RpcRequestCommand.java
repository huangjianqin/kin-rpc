package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.kin.kinrpc.transport.RequestIdGenerator;
import org.kin.kinrpc.transport.TransportConstants;
import org.kin.transport.netty.utils.VarIntUtils;

import java.util.Arrays;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.RPC_REQUEST)
public class RpcRequestCommand extends RequestCommand {
    private static final long serialVersionUID = 5549418324792160032L;
    private static final Object[] EMPTY_PARAMS = new Object[0];

    /** 服务唯一id */
    private int serviceId;
    /** 服务方法唯一id */
    private int handlerId;
    /** 服务方法调用参数 */
    private Object[] params;
    /** 服务方法调用参数bytes payload */
    private ByteBuf paramsPayload;

    public RpcRequestCommand() {
    }

    public RpcRequestCommand(byte serializationCode,
                             int serviceId,
                             int handlerId,
                             Object[] params) {
        this(TransportConstants.VERSION, serializationCode, serviceId, handlerId, params);
    }

    public RpcRequestCommand(byte serializationCode,
                             int serviceId,
                             int handlerId,
                             long timeout,
                             Object[] params) {
        this(TransportConstants.VERSION, serializationCode, serviceId, handlerId, timeout, params);
    }

    public RpcRequestCommand(short version,
                             byte serializationCode,
                             int serviceId,
                             int handlerId,
                             Object[] params) {
        this(version, serializationCode, serviceId, handlerId, 0, params);
    }

    public RpcRequestCommand(short version,
                             byte serializationCode,
                             int serviceId,
                             int handlerId,
                             long timeout,
                             Object[] params) {
        super(CommandCodes.RPC_REQUEST, version, RequestIdGenerator.next(), serializationCode);
        setTimeout(timeout);
        this.serviceId = serviceId;
        this.handlerId = handlerId;
        this.params = params;
    }

    @Override
    public void serialize(ByteBuf out) {
        super.serialize(out);
        /*
         * 变长int(1-5): serviceId
         * 变长int(1-5): handlerId
         * bytes(other): params payload
         */
        VarIntUtils.writeRawVarInt32(out, serviceId);
        VarIntUtils.writeRawVarInt32(out, handlerId);
        getSerialization().serialize(out, params);
    }

    @Override
    public void deserialize0(ByteBuf in) {
        super.deserialize0(in);
        //serviceId
        serviceId = VarIntUtils.readRawVarInt32(in);

        //handlerId
        handlerId = VarIntUtils.readRawVarInt32(in);

        //slice
        paramsPayload = in.retainedSlice();
    }

    /**
     * 反序列化服务方法params
     *
     * @param paramTypes 服务方法param类型
     */
    public void deserializeParams(Class<?>... paramTypes) {
        try {
            if (paramsPayload.readableBytes() > 1) {
                params = getSerialization().deserialize(paramsPayload, paramTypes);
            } else {
                params = EMPTY_PARAMS;
            }
        } finally {
            ReferenceCountUtil.safeRelease(paramsPayload);
            paramsPayload = null;
        }
    }

    //getter
    public int getServiceId() {
        return serviceId;
    }

    public int getHandlerId() {
        return handlerId;
    }

    public Object[] getParams() {
        return params;
    }

    public ByteBuf getParamsPayload() {
        return paramsPayload;
    }

    @Override
    public String toString() {
        return "RpcRequestCommand{" +
                "serviceId='" + serviceId + '\'' +
                ", handlerId='" + handlerId + '\'' +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
