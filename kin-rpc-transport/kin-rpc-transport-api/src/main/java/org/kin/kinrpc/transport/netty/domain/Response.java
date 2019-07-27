package org.kin.kinrpc.transport.netty.domain;

import io.netty.buffer.ByteBuf;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public interface Response {
    Response setProtocolId(int protocolId);

    ByteBuf getByteBuf();

    int getProtocolId();

    int getSize();

    Response writeByte(int value);

    Response writeUnsignedByte(short value);

    Response writeBoolean(boolean value);

    Response writeBytes(byte[] value);

    Response writeShort(int value);

    Response writeUnsignedShort(int value);

    Response writeInt(int value);

    Response writeUnsignedInt(long value);

    Response writeFloat(float value);

    Response writeLong(long value);

    Response writeDouble(double value);

    Response writeString(String value);

    Response writeBigString(String value);

    Response setBoolean(int index, boolean value);

    Response setByte(int index, int value);

    Response setUnsignedByte(int index, int value);

    Response setShort(int index, int value);

    Response setUnsignedShort(int index, int value);

    Response setInt(int index, int value);

    Response setUnsignedInt(int index, long value);

    Response setLong(int index, long value);

    Response setFloat(int index, float value);

    Response setDouble(int index, double value);

    Response setBytes(int index, byte[] value);
}
