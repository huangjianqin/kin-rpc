package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.transport.RequestIdGenerator;
import org.kin.kinrpc.transport.TransportConstants;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@CommandCode(CommandCodes.MESSAGE)
public final class MessageCommand extends RequestCommand {
    private static final long serialVersionUID = 2678295231039867756L;
    /** receiver identity */
    private String identity;
    /**
     * interest, 用于寻找唯一的{@link org.kin.kinrpc.transport.RequestProcessor}实例
     * 往往是全限定类名
     */
    private String interest;
    /** 通过{@link #interest}解析出的data class */
    private Class<? extends Serializable> dataClass;
    /**
     * data payload反序列化后的对象
     *
     * @see RemotingCommand#getPayload()
     */
    private Serializable data;

    public MessageCommand() {
    }

    public MessageCommand(byte serializationCode, String identity, Serializable data) {
        this(TransportConstants.VERSION, serializationCode, identity, data);
    }

    public MessageCommand(short version, byte serializationCode, String identity, Serializable data) {
        this(version, RequestIdGenerator.next(), serializationCode, identity, data);
    }

    private MessageCommand(short version, long id, byte serializationCode, String identity, Serializable data) {
        super(CommandCodes.MESSAGE, version, id, serializationCode);
        this.identity = identity;
        this.interest = data.getClass().getName();
        this.dataClass = data.getClass();
        this.data = data;
    }

    public MessageCommand(MessageCommand command, Serializable data) {
        // TODO: 2023/6/7 identity是否需要修改
        this(TransportConstants.VERSION, command.getId(), command.getSerializationCode(), command.identity, data);
    }

    @Override
    public void serializePayload(ByteBuf out) {
        super.serializePayload(out);
        /*
         * short(2): receiver identity len
         * bytes(identity len): receiver identity content
         * short(2): interest len
         * bytes(interest len): interest content
         * bytes(other): data payload
         */
        //receiver identity
        BytebufUtils.writeShortString(out, identity);
        //interest
        BytebufUtils.writeShortString(out, interest);

        getSerialization().serialize(out, data);
    }

    @Override
    public void deserialize0(ByteBuf in) {
        super.deserialize0(in);

        //receiver identity
        identity = BytebufUtils.readShortString(in);

        //interest
        interest = BytebufUtils.readShortString(in);

        dataClass = ClassUtils.getClass(interest);
        data = getSerialization().deserialize(in, dataClass);
    }

    //setter && getter
    public String getIdentity() {
        return identity;
    }

    public String getInterest() {
        return interest;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getData() {
        return (T) data;
    }
}
