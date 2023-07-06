package org.kin.kinrpc.transport.cmd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.transport.CommandHelper;
import org.kin.transport.netty.AdaptiveOutputByteBufAllocator;
import org.kin.transport.netty.utils.VarIntUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * outbound command编码或者inbound byte buffer解码入口
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RemotingCodec {
    private static final Logger log = LoggerFactory.getLogger(RemotingCodec.class);

    /** 自适应分配{@link io.netty.buffer.ByteBuf}实例 */
    private final AdaptiveOutputByteBufAllocator.Handle adaptiveHandle = AdaptiveOutputByteBufAllocator.DEFAULT.newHandle();
    private final ByteBufAllocator allocator;

    public RemotingCodec() {
        this(ByteBufAllocator.DEFAULT);
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
         * var int: payload len
         * var int: metadata len
         * bytes(payload len): payload(depend on actual command)
         * bytes(metadata len): metadata(Map<String, String>)
         */
        ByteBuf out = adaptiveHandle.allocate(allocator);
        ByteBuf payloadOut = null;
        ByteBuf metadataOut = null;
        try {
            out.writeByte(cmd.getCmdCode());
            out.writeByte(cmd.getVersion());
            VarIntUtils.writeRawVarInt64(out, cmd.getId(), true);
            out.writeShort(cmd.getFlag());

            payloadOut = adaptiveHandle.allocate(allocator);
            cmd.serializePayload(payloadOut);

            Map<String, String> metadata = cmd.getMetadata();
            if (CollectionUtils.isNonEmpty(metadata)) {
                metadataOut = adaptiveHandle.allocate(allocator);
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    BytebufUtils.writeVarInt32String(metadataOut, key);
                    BytebufUtils.writeVarInt32String(metadataOut, value);
                }
            }

            int payloadLen = payloadOut.readableBytes();
            VarIntUtils.writeRawVarInt32(out, payloadLen);
            int metadataLen = Objects.nonNull(metadataOut) ? metadataOut.readableBytes() : 0;
            VarIntUtils.writeRawVarInt32(out, metadataLen);
            if (payloadLen > 0) {
                out.writeBytes(payloadOut);
            }
            if (metadataLen > 0) {
                out.writeBytes(metadataOut);
            }
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(out);
            throw new CodecException("remoting codec encode fail", e);
        } finally {
            if (Objects.nonNull(payloadOut)) {
                ReferenceCountUtil.safeRelease(payloadOut);
            }

            if (Objects.nonNull(metadataOut)) {
                ReferenceCountUtil.safeRelease(metadataOut);
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
            throw new CodecException("input byte buffer is empty");
        }

        try {
            short cmdCode = in.readUnsignedByte();
            RemotingCommand command = CommandHelper.createCommandByCode(cmdCode);
            command.setVersion(in.readUnsignedByte());
            command.setId(VarIntUtils.readRawVarInt64(in, true));
            command.setFlag(in.readShort());

            int payloadLen = VarIntUtils.readRawVarInt32(in);
            int metadataLen = VarIntUtils.readRawVarInt32(in);
            command.setPayload(in.retainedSlice(in.readerIndex(), payloadLen));

            command.deserializePayload();

            //skip data payload
            in.readerIndex(in.readerIndex() + payloadLen);

            Map<String, String> metadata = Collections.emptyMap();
            if (metadataLen > 0) {
                metadata = new HashMap<>();
                while (in.readableBytes() > 0) {
                    String key = BytebufUtils.readVarInt32String(in);
                    String value = BytebufUtils.readVarInt32String(in);

                    metadata.put(key, value);
                }
            }
            command.setMetadata(metadata);

            return command;
        }catch (Exception e) {
            throw new CodecException("remoting codec decode fail", e);
        } finally {
            ReferenceCountUtil.safeRelease(in);
        }
    }
}
