package org.kin.kinrpc.transport;

import io.netty.buffer.ByteBuf;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.transport.cmd.RemotingCodec;
import org.kin.kinrpc.transport.cmd.RemotingCommand;
import org.kin.kinrpc.transport.cmd.processor.*;

import java.util.*;

/**
 * protocol byte buffer统一处理入口
 *
 * @author huangjianqin
 * @date 2023/5/31
 */
public class RemotingProcessor {
    /** key -> command code, value -> {@link CommandProcessor}实例 */
    private final Map<Short, CommandProcessor<RemotingCommand>> cmdProcessorMap = new HashMap<>();
    /** command processor线程池 */
    private final ExecutionContext executor = ExecutionContext.elastic(SysUtils.CPU_NUM, SysUtils.DOUBLE_CPU, "command-processor");
    /** 协议codec */
    private final RemotingCodec codec;
    /** {@link RequestProcessor}实例管理 */
    private final RequestProcessorManager requestProcessorManager = new RequestProcessorManager();

    @SuppressWarnings("unchecked")
    public RemotingProcessor(RemotingCodec codec) {
        this.codec = codec;

        List<CommandProcessor<? extends RemotingCommand>> commandProcessors = Arrays.asList(new HeartbeatCommandProcessor(),
                new MessageCommandProcessor(),
                new RpcRequestCommandProcessor(),
                new RpcResponseCommandProcessor());
        for (CommandProcessor<? extends RemotingCommand> commandProcessor : commandProcessors) {
            Class<? extends RemotingCommand> type = (Class<?extends RemotingCommand>)
                    ClassUtils.getSuperInterfacesGenericActualTypes(CommandProcessor.class, commandProcessor.getClass()).get(0);
            short commandCode = CommandHelper.getCommandCode(type);
            cmdProcessorMap.put(commandCode, (CommandProcessor<RemotingCommand>) commandProcessor);
        }
    }

    /**
     * process protocol byte buffer
     *
     * @param context channel context
     * @param in      protocol byte buffer
     */
    public void process(ChannelContext context, ByteBuf in) {
        executor.execute(new CommandProcessTask(context, in));
    }

    public void shutdown() {
        executor.shutdown();
    }

    //---------------------------------------------------------------------------------------------------------------------------------------
    private class CommandProcessTask implements Runnable {
        /** channel context */
        private final ChannelContext channelContext;
        /** protocol byte buffer */
        private final ByteBuf in;

        public CommandProcessTask(ChannelContext channelContext, ByteBuf in) {
            this.channelContext = channelContext;
            this.in = in;
        }

        @Override
        public void run() {
            RemotingCommand command = codec.decode(in);
            short cmdCode = command.getCmdCode();
            CommandProcessor<RemotingCommand> processor = cmdProcessorMap.get(cmdCode);
            if (Objects.isNull(processor)) {
                throw new TransportException("can not find command processor with command code " + cmdCode);
            }

            processor.process(new RemotingContext(codec, requestProcessorManager, channelContext), command);
        }
    }

    //getter
    public RequestProcessorManager getRequestProcessorManager() {
        return requestProcessorManager;
    }
}
