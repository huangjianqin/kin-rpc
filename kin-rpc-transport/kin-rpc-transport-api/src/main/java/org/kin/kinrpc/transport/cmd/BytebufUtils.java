package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import org.kin.transport.netty.utils.VarIntUtils;

import java.nio.charset.StandardCharsets;

/**
 * byte buffer工具类
 *
 * @author huangjianqin
 * @date 2023/6/3
 */
public final class BytebufUtils {
    private BytebufUtils() {
    }

    /**
     * write short len based string
     *
     * @param byteBuf byte buffer
     * @param s       字符串
     */
    public static void writeShortString(ByteBuf byteBuf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byteBuf.writeShort(bytes.length);
        byteBuf.writeBytes(bytes);
    }

    /**
     * read short len based string
     *
     * @param byteBuf byte buffer
     * @return decoded字符串
     */
    public static String readShortString(ByteBuf byteBuf) {
        short len = byteBuf.readShort();
        byte[] bytes = new byte[len];
        byteBuf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * write var int32 len based string
     *
     * @param byteBuf byte buffer
     * @param s       字符串
     */
    public static void writeVarInt32String(ByteBuf byteBuf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        VarIntUtils.writeRawVarInt32(byteBuf, bytes.length);
        byteBuf.writeBytes(bytes);
    }

    /**
     * read var int32 len based string
     *
     * @param byteBuf byte buffer
     * @return decoded字符串
     */
    public static String readVarInt32String(ByteBuf byteBuf) {
        int len = VarIntUtils.readRawVarInt32(byteBuf);
        byte[] bytes = new byte[len];
        byteBuf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
