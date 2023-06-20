package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.kin.kinrpc.transport.RequestIdGenerator;
import org.kin.kinrpc.transport.TransportConstants;

import java.util.Arrays;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.RPC_REQUEST)
public class RpcRequestCommand extends RequestCommand {
    private static final long serialVersionUID = 5549418324792160032L;
    private static final Object[] EMPTY_PARAMS = new Object[0];

    /** 服务唯一标识 */
    private String gsv;
    /** 服务方法 */
    private String method;
    /** 服务方法调用参数 */
    private Object[] params;
    /** 服务方法调用参数bytes payload */
    private ByteBuf paramsPayload;

    public RpcRequestCommand() {
    }

    public RpcRequestCommand(byte serializationCode,
                             String gsv, String method, Object[] params) {
        this(TransportConstants.VERSION, serializationCode, gsv, method, params);
    }

    public RpcRequestCommand(short version, byte serializationCode,
                             String gsv, String method, Object[] params) {
        super(CommandCodes.RPC_REQUEST, version, RequestIdGenerator.next(), serializationCode);
        this.gsv = gsv;
        this.method = method;
        this.params = params;
    }

    @Override
    public void serialize(ByteBuf out) {
        super.serialize(out);
        /*
         * short(2): gsv len
         * bytes(gsv len): gsv content
         * short(2): method len
         * bytes(method len): method content
         * bytes(other): params payload
         */
        BytebufUtils.writeShortString(out, gsv);
        BytebufUtils.writeShortString(out, method);
        getSerialization().serialize(out, params);
    }

    @Override
    public void deserialize0(ByteBuf in) {
        super.deserialize0(in);
        //gsv
        gsv = BytebufUtils.readShortString(in);

        //method
        method = BytebufUtils.readShortString(in);

        //slice
        paramsPayload = in.retainedSlice();
    }

    /**
     * 反序列化服务方法params
     * @param paramTypes    服务方法param类型
     */
    public void deserializeParams(Class<?>... paramTypes) {
        try {
            if (paramsPayload.readableBytes() > 1) {
                params = getSerialization().deserialize(paramsPayload, paramTypes);
            } else {
                params = EMPTY_PARAMS;
            }
        }finally {
            ReferenceCountUtil.safeRelease(paramsPayload);
            paramsPayload = null;
        }
    }

    //getter
    public String getGsv() {
        return gsv;
    }

    public String getMethod() {
        return method;
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
                "gsv='" + gsv + '\'' +
                ", method='" + method + '\'' +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
