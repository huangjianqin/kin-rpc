package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.cmd.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author huangjianqin
 * @date 2023/5/31
 */
public final class CommandHelper {
    private CommandHelper() {
    }

    /**
     * command factory map
     * key -> command code, value -> command factory
     */
    private static final Map<Short, Supplier<RemotingCommand>> COMMAND_FACTORIES = new HashMap<>();

    static {
        registerFactory(HeartbeatCommand.class, HeartbeatCommand::new);
        registerFactory(MessageCommand.class, MessageCommand::new);
        registerFactory(RpcRequestCommand.class, RpcRequestCommand::new);
        registerFactory(RpcResponseCommand.class, RpcResponseCommand::new);

    }

    /**
     * 注册command factory
     * @param type  command type
     * @param factory   command factory
     */
    @SuppressWarnings("unchecked")
    public static <RC extends RemotingCommand> void registerFactory(Class<RC> type, Supplier<RC> factory){
        short code = getCommandCode(type);
        if(COMMAND_FACTORIES.containsKey(code)){
            throw new TransportException(String.format("command type '%s' has been registered", type.getName()));
        }

        COMMAND_FACTORIES.put(code, (Supplier<RemotingCommand>) factory);
    }

    /**
     * 从command type中获取command code
     * @param type  command type
     * @return  command code
     */
    public static <RC extends RemotingCommand> short getCommandCode(Class<RC> type){
        CommandCode commandCode = type.getAnnotation(CommandCode.class);
        if (Objects.isNull(commandCode)) {
            throw new TransportException(String.format("command type '%s' miss @%s", type.getName(), CommandCode.class.getSimpleName()));
        }

        return commandCode.value();
    }

    /**
     * 根据command code创建{@link RemotingCommand}实例
     * @param cmdCode   command code
     * @return  {@link RemotingCommand}实例
     */
    @SuppressWarnings("unchecked")
    public static <RC extends RemotingCommand> RC createCommandByCode(short cmdCode) {
        Supplier<RemotingCommand> factory = COMMAND_FACTORIES.get(cmdCode);
        if (Objects.isNull(factory)) {
            throw new TransportException("can not find command factory with command code " + cmdCode);
        }

        RC rc = (RC) factory.get();
        rc.setCmdCode(cmdCode);
        return rc;
    }
}
