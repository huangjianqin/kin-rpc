package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RemotingCommand;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * user defined request process executor selector
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface ExecutorSelector {
    /**
     * user defined request process executor select逻辑
     * @param command   request command
     * @return  user defined request process executor
     */
    <RC extends RemotingCommand> Executor select(RC command);

    /**
     * 固定executor
     * @param executor  executor
     * @return selector which always return {@code executor}
     */
    static ExecutorSelector fix(Executor executor) {
        return new ExecutorSelector() {
            @Override
            public <RC extends RemotingCommand> Executor select(RC command) {
                return executor;
            }
        };
    }
}
