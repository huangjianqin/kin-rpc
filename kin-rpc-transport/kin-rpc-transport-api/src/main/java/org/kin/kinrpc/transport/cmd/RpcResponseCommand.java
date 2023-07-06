package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.kin.kinrpc.transport.TransportException;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.RPC_RESPONSE)
public class RpcResponseCommand extends RemotingCommand {
    /** 服务调用状态 */
    private Status status;
    /**
     * 服务调用结果
     * 当服务调用异常, response返回string
     */
    private Object result;
    /** 服务方法调用结果bytes payload */
    private ByteBuf resultPayload;

    public static RpcResponseCommand success(RemotingCommand command, Object result){
        return success(command.getVersion(), command.getId(),
                command.getSerializationCode(), result);
    }

    public static RpcResponseCommand success(short version, long id, byte serializationCode,
                                             Object result){
        return new RpcResponseCommand(version, id, serializationCode, Status.SUCCESS, result);
    }

    public static RpcResponseCommand error(RemotingCommand command, String errorMsg){
        return error(command.getVersion(), command.getId(),
                command.getSerializationCode(), errorMsg);
    }

    public static RpcResponseCommand error(short version, long id, byte serializationCode, String errorMsg){
        return new RpcResponseCommand(version, id, serializationCode, Status.ERROR, errorMsg);
    }

    public RpcResponseCommand() {
    }

    public RpcResponseCommand(short version, long id, byte serializationCode,
                              Status status, Object result) {
        super(CommandCodes.RPC_RESPONSE, version, id);
        setSerializationCode(serializationCode);
        this.status = status;
        this.result = result;
    }

    @Override
    public void serializePayload(ByteBuf out) {
        /*
         * byte: response status
         * bytes(other): rpc call result
         */

        out.writeByte(status.getCode());
        if (Objects.nonNull(result)) {
            out.writeBytes(getSerialization().serialize(result));
        }
    }

    @Override
    public void deserialize0(ByteBuf payload) {
        status = Status.getByCode(payload.readByte());
        //slice
        resultPayload = payload.retainedSlice();
    }

    /**
     * 反序列化服务方法调用结果
     * @param resultType    服务方法返回值类型
     */
    public void deserializeResult(Class<?> resultType) {
        try{
            if (resultPayload.readableBytes() > 0) {
                result = getSerialization().deserialize(resultPayload, resultType);
            } else {
                result = null;
            }
        }finally {
            ReferenceCountUtil.safeRelease(resultPayload);
            resultPayload = null;
        }
    }

    /**
     * rpc request是否调用成功
     * @return  true表示rpc request调用成功
     */
    public boolean isOk(){
        return Status.SUCCESS.equals(status);
    }

    //getter
    public Status getStatus() {
        return status;
    }

    public Object getResult() {
        return result;
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------
    public enum Status {
        /**
         * 服务调用成功
         */
        SUCCESS(0),
        /**
         * 服务调用错误
         */
        ERROR(1),
        ;

        private static final Status[] VALUES = values();

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        /**
         * 根据{@code code}寻找{@link Status}枚举
         * @return  {@link Status}枚举
         */
        public static Status getByCode(int code){
            for (Status status : VALUES) {
                if(status.getCode() == code){
                    return status;
                }
            }

            throw new TransportException("can not find response status with code " + code);
        }
    }
}
