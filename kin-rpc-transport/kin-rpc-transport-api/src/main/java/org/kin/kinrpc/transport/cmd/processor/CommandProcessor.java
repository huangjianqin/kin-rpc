package org.kin.kinrpc.transport.cmd.processor;

import org.kin.kinrpc.transport.RemotingContext;
import org.kin.kinrpc.transport.RequestContext;
import org.kin.kinrpc.transport.cmd.RemotingCommand;

/**
 * @author huangjianqin
 * @date 2023/5/31
 */
public interface CommandProcessor<C extends RemotingCommand> {
    /**
     * command处理逻辑
     * @param context   remoting context
     * @param command   command
     */
    void process(RemotingContext context, C command);
}
