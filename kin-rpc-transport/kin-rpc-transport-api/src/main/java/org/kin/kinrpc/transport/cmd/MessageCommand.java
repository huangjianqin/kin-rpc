package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.transport.TransportException;

import java.nio.charset.StandardCharsets;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.MESSAGE)
public final class MessageCommand extends RequestCommand {
    private static final long serialVersionUID = 2678295231039867756L;
    /** receiver name */
    private String name;
    /**
     * interest, 用于寻找唯一的{@link org.kin.kinrpc.transport.RequestProcessor}实例
     * 往往是全限定类名
     */
    private String interest;
    /** 通过{@link #interest}解析出的data class */
    private Class<?> dataClass;
    /**
     * data payload反序列化后的对象
     * @see RemotingCommand#getPayload()
     */
    private Object data;

    public MessageCommand() {
    }

    public MessageCommand(short version, long id, byte serializationCode, String name, Object data) {
        super(CommandCodes.MESSAGE, version, id, serializationCode);
        this.name = name;
        this.interest = data.getClass().getName();
        this.dataClass = data.getClass();
        this.data = data;
    }

    @Override
    public void serialize(ByteBuf out) {
        super.serialize(out);
        /*
         * short(2): name len
         * bytes(name len): name content
         * short(2): interest len
         * bytes(interest len): interest content
         * bytes(other): data payload
         */
        //name
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        out.writeShort(nameBytes.length);
        out.writeBytes(nameBytes);
        //interest
        byte[] interestBytes = interest.getBytes(StandardCharsets.UTF_8);
        out.writeShort(interestBytes.length);
        out.writeBytes(interestBytes);

        getSerialization().serialize(out, data);
    }

    @Override
    public void deserialize0(ByteBuf payload) {
        super.deserialize();

        //name
        short nameLen = payload.readShort();
        if(nameLen < 0){
            throw new TransportException("invalid message command, due to miss receiver name");
        }
        byte[] nameBytes = new byte[nameLen];
        name = new String(nameBytes, StandardCharsets.UTF_8);

        //interest
        short interestLen = payload.readShort();
        if(interestLen < 0){
            throw new TransportException("invalid message command, due to miss interest");
        }
        byte[] interestBytes = new byte[interestLen];
        interest = new String(interestBytes, StandardCharsets.UTF_8);

        dataClass = ClassUtils.getClass(interest);
        data = getSerialization().deserialize(payload, dataClass);
    }

    //setter && getter
    public String getName() {
        return name;
    }

    public String getInterest() {
        return interest;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }
}
