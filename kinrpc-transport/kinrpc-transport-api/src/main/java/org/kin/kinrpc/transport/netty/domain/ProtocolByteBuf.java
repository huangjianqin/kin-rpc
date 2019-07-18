package org.kin.kinrpc.transport.netty.domain;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;

import java.nio.charset.Charset;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public class ProtocolByteBuf implements Request, Response, ReferenceCounted {
    private static final int READ_MODE = 0;
    private static final int WRITE_MODE = 1;
    private static final int READ_WRITE_MODE = 2;

    private ByteBuf byteBuf;
    private int respSN = -1;
    private int protocolId;
    private int contentSize;
    private final int mode;

    public ProtocolByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
        this.protocolId = byteBuf.readUnsignedShort();
        this.contentSize = byteBuf.readableBytes();
        this.mode = READ_MODE;
    }

    public ProtocolByteBuf(ByteBuf byteBuf, boolean needReadRespSN) {
        this.byteBuf = byteBuf;
        if(needReadRespSN){
            this.respSN = byteBuf.readInt();
        }
        this.protocolId = byteBuf.readUnsignedShort();
        this.contentSize = byteBuf.readableBytes();
        this.mode = READ_MODE;
    }

    public ProtocolByteBuf(int protocolId) {
        byteBuf = Unpooled.buffer();
        byteBuf.writeShort(protocolId);
        this.protocolId = protocolId;
        this.mode = WRITE_MODE;
    }

    //--------------------------------------------request----------------------------------------------------
    @Override
    public int getRespSN() {
        return respSN;
    }

    @Override
    public int getContentSize() {
        Preconditions.checkArgument(mode == READ_MODE);
        return contentSize;
    }

    @Override
    public byte readByte() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readByte();
    }

    @Override
    public short readUnsignedByte() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readUnsignedByte();
    }

    @Override
    public boolean readBoolean() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readBoolean();
    }

    @Override
    public byte[] readBytes(int length) {
        Preconditions.checkArgument(mode == READ_MODE);
        Preconditions.checkArgument(length > 0);
        Preconditions.checkArgument(length <= byteBuf.readableBytes());
        byte[] result = new byte[length];
        byteBuf.readBytes(result);
        return result;
    }

    @Override
    public byte[] readBytes() {
        Preconditions.checkArgument(mode == READ_MODE);
        byte[] result = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(result);
        return result;
    }

    @Override
    public short readShort() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readShort();
    }

    @Override
    public int readUnsignedShort() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readUnsignedShort();
    }

    @Override
    public int readInt() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readInt();
    }

    @Override
    public long readUnsignedInt() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readUnsignedInt();
    }

    @Override
    public float readFloat() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readFloat();
    }

    @Override
    public long readLong() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readLong();
    }

    @Override
    public double readDouble() {
        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.readDouble();
    }

    @Override
    public String readString() {
        Preconditions.checkArgument(mode == READ_MODE);
        int length = byteBuf.readShort();
        byte[] content = new byte[length];
        byteBuf.readBytes(content);
        return new String(content, Charset.forName("UTF-8"));
    }

    @Override
    public String readBigString() {
        Preconditions.checkArgument(mode == READ_MODE);
        int length = byteBuf.readUnsignedShort();
        byte[] content = new byte[length];
        byteBuf.readBytes(content);
        return new String(content, Charset.forName("UTF-8"));
    }


    @Override
    public int refCnt() {
//        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
//        Preconditions.checkArgument(mode == READ_MODE);
        byteBuf.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int i) {
//        Preconditions.checkArgument(mode == READ_MODE);
        byteBuf.retain(i);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
//        Preconditions.checkArgument(mode == READ_MODE);
        byteBuf.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object o) {
//        Preconditions.checkArgument(mode == READ_MODE);
        byteBuf.touch(o);
        return this;
    }

    @Override
    public boolean release() {
//        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.release();
    }

    @Override
    public boolean release(int i) {
//        Preconditions.checkArgument(mode == READ_MODE);
        return byteBuf.release(i);
    }

    //--------------------------------------------response----------------------------------------------------
    @Override
    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    @Override
    public Response setProtocolId(int protocolId) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        byteBuf.setShort(0, protocolId);
        return this;
    }

    @Override
    public int getProtocolId() {
        return protocolId;
    }

    @Override
    public int getSize() {
        Preconditions.checkArgument(mode == WRITE_MODE);
        return byteBuf.readableBytes();
    }

    @Override
    public Response writeByte(int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE, "value: %s", value);
        byteBuf.writeByte(value);
        return this;
    }

    @Override
    public Response writeUnsignedByte(short value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= 0 && value <= Byte.MAX_VALUE - Byte.MIN_VALUE, "value: %s", value);
        byteBuf.writeByte(value);
        return this;
    }

    @Override
    public Response writeBoolean(boolean value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        byteBuf.writeByte(value ? 1 : 0);
        return this;
    }

    @Override
    public Response writeBytes(byte[] value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value != null);
        byteBuf.writeBytes(value);
        return this;
    }

    @Override
    public Response writeShort(int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE, "value: %s", value);
        byteBuf.writeShort(value);
        return this;
    }

    @Override
    public Response writeUnsignedShort(int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= 0 && value <= Short.MAX_VALUE - Short.MIN_VALUE, "value: %s", value);
        byteBuf.writeShort(value);
        return this;
    }

    @Override
    public Response writeInt(int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE, "value: %s", value);
        byteBuf.writeInt(value);
        return this;
    }

    @Override
    public Response writeUnsignedInt(long value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= 0 && value <= Integer.MAX_VALUE - Integer.MIN_VALUE, "value: %s", value);
        byteBuf.writeInt((int) value);
        return this;
    }

    @Override
    public Response writeFloat(float value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Float.MIN_VALUE && value <= Float.MAX_VALUE, "value: %s", value);
        byteBuf.writeFloat(value);
        return this;
    }

    @Override
    public Response writeLong(long value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Long.MIN_VALUE && value <= Long.MAX_VALUE, "value: %s", value);
        byteBuf.writeLong(value);
        return this;
    }

    @Override
    public Response writeDouble(double value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Double.MIN_VALUE && value <= Double.MAX_VALUE, "value: %s", value);
        byteBuf.writeDouble(value);
        return this;
    }

    private void writeString0(String value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value != null);
        byte[] content = value.getBytes(Charset.forName("UTF-8"));
        byteBuf.writeShort(content.length);
        byteBuf.writeBytes(content);
    }

    @Override
    public Response writeString(String value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        writeString0(value);
        return this;
    }

    @Override
    public Response writeBigString(String value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        writeString0(value);
        return this;
    }

    @Override
    public Response setBoolean(int index, boolean value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        byteBuf.setBoolean(index, value);
        return this;
    }

    @Override
    public Response setByte(int index, int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE, "value: %s", value);
        byteBuf.setByte(index, value);
        return this;
    }

    @Override
    public Response setUnsignedByte(int index, int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= 0 && value <= Byte.MAX_VALUE - Byte.MIN_VALUE, "value: %s", value);
        byteBuf.setByte(index, value);
        return this;
    }

    @Override
    public Response setShort(int index, int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE, "value: %s", value);
        byteBuf.setShort(index, value);
        return this;
    }

    @Override
    public Response setUnsignedShort(int index, int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= 0 && value <= Short.MAX_VALUE - Short.MIN_VALUE, "value: %s", value);
        byteBuf.setShort(index, value);
        return this;
    }

    @Override
    public Response setInt(int index, int value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE, "value: %s", value);
        byteBuf.setInt(index, value);
        return this;
    }

    @Override
    public Response setUnsignedInt(int index, long value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= 0 && value <= Integer.MAX_VALUE - Integer.MIN_VALUE, "value: %s", value);
        byteBuf.setInt(index, (int) value);
        return this;
    }

    @Override
    public Response setLong(int index, long value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Long.MIN_VALUE && value <= Long.MAX_VALUE, "value: %s", value);
        byteBuf.setLong(index, value);
        return this;
    }

    @Override
    public Response setFloat(int index, float value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Float.MIN_VALUE && value <= Float.MAX_VALUE, "value: %s", value);
        byteBuf.setFloat(index, value);
        return this;
    }

    @Override
    public Response setDouble(int index, double value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value >= Double.MIN_VALUE && value <= Double.MAX_VALUE, "value: %s", value);
        byteBuf.setDouble(index, value);
        return this;
    }

    @Override
    public Response setBytes(int index, byte[] value) {
        Preconditions.checkArgument(mode == WRITE_MODE);
        Preconditions.checkArgument(value != null);
        byteBuf.setBytes(index, value);
        return this;
    }
}
