package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.kin.serialization.Serialization;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public abstract class RemotingCommand implements Serializable {
    private static final long serialVersionUID = -4285886763714528091L;

    /** serialization标识mask */
    private static final int FLAG_SERIALIZATION_MASK = 0xF000;
    /** serialization标识位移 */
    private static final int FLAG_SERIALIZATION_SHIFT = 12;

    /** command code */
    private short cmdCode;
    /** command version */
    private short version;
    /** command id */
    private long id;
    /** command flag */
    private short flag;
    /** serialization code */
    private byte serializationCode;
    /** data payload, 一般{@link #deserializePayload()}为会set为null */
    private ByteBuf payload = Unpooled.EMPTY_BUFFER;
    /** command metadata */
    private Map<String, String> metadata = Collections.emptyMap();

    protected RemotingCommand() {
    }

    protected RemotingCommand(short cmdCode, short version, long id) {
        this.cmdCode = cmdCode;
        this.version = version;
        this.id = id;
    }

    /**
     * 序列化payload
     *
     * @param out write out byte buffer
     */
    public abstract void serializePayload(ByteBuf out);

    /**
     * 反序列化payload payload
     */
    public final void deserializePayload() {
        if (Objects.isNull(payload)) {
            //已经反序列化了
            return;
        }

        try {
            deserialize0(payload);
        } finally {
            ReferenceCountUtil.safeRelease(payload);
            setPayload(null);
        }
    }

    /**
     * 反序列化payload payload
     * @param payload   payload payload
     */
    protected abstract void deserialize0(ByteBuf payload);

    //setter && getter
    public final short getVersion() {
        return version;
    }

    public final short getCmdCode() {
        return cmdCode;
    }

    public final long getId() {
        return id;
    }

    public short getFlag() {
        return flag;
    }

    public final byte getSerializationCode() {
        return serializationCode;
    }

    public final Serialization getSerialization() {
        return Serializations.getByCode(serializationCode);
    }

    protected ByteBuf getPayload() {
        return payload;
    }

    public final Map<String, String> getMetadata() {
        return metadata;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public void setCmdCode(short cmdCode) {
        this.cmdCode = cmdCode;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setFlag(short flag) {
        this.flag = flag;
        this.serializationCode = (byte) ((flag & FLAG_SERIALIZATION_MASK) >>> FLAG_SERIALIZATION_SHIFT);
    }

    public void setSerializationCode(byte serializationCode) {
        this.serializationCode = serializationCode;
        this.flag = (short) (flag | (((short) serializationCode) << FLAG_SERIALIZATION_SHIFT));
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
