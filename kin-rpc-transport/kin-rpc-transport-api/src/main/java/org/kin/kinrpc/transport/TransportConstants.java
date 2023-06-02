package org.kin.kinrpc.transport;

import java.nio.charset.StandardCharsets;

/**
 * transport相关常量
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface TransportConstants {
    /** 版本号 */
    short VERSION = 1;
    /** 魔数 */
    String MAGIC = "KinRPC";
    /** 魔数字节数组 */
    byte[] MAGIC_BYTES = MAGIC.getBytes(StandardCharsets.UTF_8);
}
