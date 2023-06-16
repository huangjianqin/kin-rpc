package org.kin.kinrpc.transport;

import io.netty.buffer.ByteBuf;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.transport.cmd.RemotingCodec;
import org.kin.kinrpc.transport.cmd.RemotingCommand;
import org.kin.kinrpc.transport.cmd.processor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * protocol byte buffer统一处理入口
 *
 * @author huangjianqin
 * @date 2023/5/31
 */
public class RemotingProcessor {
    private static final Logger log = LoggerFactory.getLogger(RemotingProcessor.class);

    /** key -> command code, value -> {@link CommandProcessor}实例 */
    private final Map<Short, CommandProcessor<RemotingCommand>> cmdProcessorMap = new HashMap<>();
    // TODO: 2023/6/12 支持配置
    /** command processor线程池 */
    private final ExecutionContext executor =
            ExecutionContext.elastic(SysUtils.CPU_NUM, SysUtils.DOUBLE_CPU,
                    new SimpleThreadFactory("command-processor", true));
    /** 协议codec */
    private final RemotingCodec codec;
    /** {@link RequestProcessor}实例管理 */
    private final RequestProcessorRegistry requestProcessorRegistry = new RequestProcessorRegistry();

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
     * 会对{@code in}进行{@link ByteBuf#release()}操作, 需保证{@link ByteBuf#refCnt()}大于0
     *
     * @param context channel context
     * @param in      protocol byte buffer
     */
    public void process(ChannelContext context, ByteBuf in) {
        executor.execute(new CommandProcessTask(context, in));
    }

    /**
     * release resource
     */
    public void shutdown() {
        executor.shutdown();
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * {@link CommandProcessor#process(RemotingContext, RemotingCommand)} task
     */
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
            RemotingCommand command;
            try {
                command = codec.decode(in);
            }catch (Exception e){
                log.error("command decode fail", e);
                throw new TransportException("command decode fail", e);
            }

            RemotingContext remotingContext = new RemotingContext(codec, requestProcessorRegistry, channelContext);
            try {
                short cmdCode = command.getCmdCode();
                CommandProcessor<RemotingCommand> processor = cmdProcessorMap.get(cmdCode);
                if (Objects.isNull(processor)) {
                    throw new RemotingException("can not find command processor with command code " + cmdCode);
                }

                processor.process(remotingContext, command);
            } catch (Exception e) {
                log.error("process command fail, {}", command, e);
                //command process fail, response error
                remotingContext.writeResponseIfError(command, e.getMessage());
            }
        }
    }

    //getter
    public RequestProcessorRegistry getRequestProcessorManager() {
        return requestProcessorRegistry;
    }
}
