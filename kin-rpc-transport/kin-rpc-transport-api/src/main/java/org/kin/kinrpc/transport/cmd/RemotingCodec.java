package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.transport.CommandHelper;
import org.kin.kinrpc.transport.TransportException;
import org.kin.transport.netty.AdaptiveOutputByteBufAllocator;
import org.kin.transport.netty.utils.VarIntUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * protocol byte buffer统一编解码
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RemotingCodec {
    private static final Logger log = LoggerFactory.getLogger(RemotingCodec.class);

    /** 自适应分配{@link io.netty.buffer.ByteBuf}实例 */
    private final AdaptiveOutputByteBufAllocator.Handle adaptiveHandle = AdaptiveOutputByteBufAllocator.DEFAULT.newHandle();
    private ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;

    public RemotingCodec() {
    }

    public RemotingCodec(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    /**
     * 编码
     * @param cmd   {@link RemotingCommand}实例
     * @return  protocol byte buffe
     */
    public ByteBuf encode(RemotingCommand cmd){
        /*
         * unsigned byte: command code
         * unsigned byte: version
         * signed var long: command id, usually request id
         * short: flag(4bit serialization code; )
         * var int: data len
         * var int: headers len
         * bytes(data len): data(depend on actual command)
         * bytes(headers len): headers(Map<String, String>)
         */
        ByteBuf out = adaptiveHandle.allocate(allocator);
        ByteBuf dataOut = null;
        ByteBuf headersOut = null;
        try {
            out.writeByte(cmd.getCmdCode());
            out.writeByte(cmd.getVersion());
            VarIntUtils.writeRawVarInt64(out, cmd.getId(), true);
            out.writeShort(cmd.getFlag());

            dataOut = adaptiveHandle.allocate(allocator);
            cmd.serialize(dataOut);

            Map<String, String> headers = cmd.getHeaders();
            if(CollectionUtils.isNonEmpty(headers)){
                headersOut = adaptiveHandle.allocate(allocator);
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    BytebufUtils.writeVarInt32String(headersOut, key);
                    BytebufUtils.writeVarInt32String(headersOut, value);
                }
            }

            int dataLen = dataOut.readableBytes();
            VarIntUtils.writeRawVarInt32(out, dataLen);
            int headersLen = Objects.nonNull(headersOut) ? headersOut.readableBytes() : 0;
            VarIntUtils.writeRawVarInt32(out, headersLen);
            if(dataLen > 0){
                out.writeBytes(dataOut);
            }
            if(headersLen > 0){
                out.writeBytes(headersOut);
            }
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(out);
            throw new TransportException("remoting codec encode fail", e);
        } finally {
            if (Objects.nonNull(dataOut)) {
                ReferenceCountUtil.safeRelease(dataOut);
            }

            if (Objects.nonNull(headersOut)) {
                ReferenceCountUtil.safeRelease(headersOut);
            }
        }

        return out;
    }

    /**
     * 解码
     * @param in    protocol byte buffer
     * @return  解析后的 {@link RemotingCommand}
     */
    public RemotingCommand decode(ByteBuf in){
        if(in.readableBytes() < 1){
            throw new TransportException("input byte buffer is empty");
        }

        try{
            short cmdCode = in.readUnsignedByte();
            RemotingCommand command = CommandHelper.createCommandByCode(cmdCode);
            command.setVersion(in.readUnsignedByte());
            command.setId(VarIntUtils.readRawVarInt64(in, true));
            command.setFlag(in.readShort());

            int dataLen = VarIntUtils.readRawVarInt32(in);
            int headersLen = VarIntUtils.readRawVarInt32(in);
            command.setPayload(in.retainedSlice(in.readerIndex(), dataLen));

            command.deserialize();

            //skip data payload
            in.readerIndex(in.readerIndex() + dataLen);

            Map<String, String> headers = Collections.emptyMap();
            if(headersLen > 0){
                headers = new HashMap<>();
                while(in.readableBytes() > 0) {
                    String key = BytebufUtils.readVarInt32String(in);
                    String value = BytebufUtils.readVarInt32String(in);

                    headers.put(key, value);
                }
            }
            command.setHeaders(headers);

            return command;
        }catch (Exception e) {
            throw new TransportException("remoting codec decode fail", e);
        } finally {
            ReferenceCountUtil.safeRelease(in);
        }
    }
}
