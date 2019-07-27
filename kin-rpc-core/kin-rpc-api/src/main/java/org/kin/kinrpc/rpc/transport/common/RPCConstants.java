package org.kin.kinrpc.rpc.transport.common;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCConstants {
    private RPCConstants(){

    }

    //TODO 暂定
    public static final int RPC_HEARTBEAT_PROTOCOL_ID = 10;
    public static final int RPC_REQUEST_PROTOCOL_ID = 11;
    public static final int RPC_RESPONSE_PROTOCOL_ID = 12;

    //provider线程池最大允许queued任务量
    public static final int POOL_TASK_NUM = 50;
}
