package org.kin.kinrpc.transport.cmd;

/**
 * command code定义
 * @author huangjianqin
 * @date 2023/5/31
 */
public interface CommandCodes {
    /** 心跳 */
    short HEARTBEAT = 0;
    /** message请求 */
    short MESSAGE = 1;
    /** rpc请求 */
    short RPC_REQUEST = 2;
    /** rpc响应 */
    short RPC_RESPONSE = 3;
}
