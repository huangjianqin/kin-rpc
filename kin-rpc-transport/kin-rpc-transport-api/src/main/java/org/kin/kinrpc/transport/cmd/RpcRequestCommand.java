package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.kin.kinrpc.transport.TransportException;

import java.nio.charset.StandardCharsets;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.RPC_REQUEST)
public class RpcRequestCommand extends RequestCommand {
    private static final long serialVersionUID = 5549418324792160032L;

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

    public RpcRequestCommand(short version, long id, byte serializationCode,
                             String gsv, String method, Object[] params) {
        super(CommandCodes.RPC_REQUEST, version, id, serializationCode);
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
        byte[] gsvBytes = gsv.getBytes(StandardCharsets.UTF_8);
        out.writeShort(gsvBytes.length);
        out.writeBytes(gsvBytes);
        byte[] methodBytes = method.getBytes(StandardCharsets.UTF_8);
        out.writeShort(methodBytes.length);
        out.writeBytes(methodBytes);
        getSerialization().serialize(out, params);
    }

    @Override
    public void deserialize0(ByteBuf payload) {
        super.deserialize();
        //gsv
        short gsvLen = payload.readShort();
        if(gsvLen < 0){
            throw new TransportException("invalid message command, due to miss gsv");
        }

        byte[] gsvBytes = new byte[gsvLen];
        gsv = new String(gsvBytes, StandardCharsets.UTF_8);

        //method
        short methodLen = payload.readShort();
        if(methodLen < 0){
            throw new TransportException("invalid message command, due to miss method");
        }

        byte[] methodBytes = new byte[methodLen];
        method = new String(methodBytes, StandardCharsets.UTF_8);
        //slice
        paramsPayload = payload.retainedSlice();
    }

    /**
     * 反序列化服务方法params
     * @param paramTypes    服务方法param类型
     */
    public void deserializeParams(Class<?>... paramTypes) {
        try{
            params = getSerialization().deserialize(paramsPayload, paramTypes);
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
}
