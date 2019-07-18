package org.kin.kinrpc.transport.netty.domain;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public interface Request {
    int getRespSN();

    int getProtocolId();

    /**
     * 获取不含协议id的协议内容字节数大小
     */
    int getContentSize();

    byte readByte();

    short readUnsignedByte();

    boolean readBoolean();

    byte[] readBytes(int length);

    byte[] readBytes();

    short readShort();

    int readUnsignedShort();

    int readInt();

    long readUnsignedInt();

    float readFloat();

    long readLong();

    double readDouble();

    String readString();

    String readBigString();
}
