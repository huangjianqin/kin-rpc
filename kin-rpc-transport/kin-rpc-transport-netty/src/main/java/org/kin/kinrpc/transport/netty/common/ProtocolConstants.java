package org.kin.kinrpc.transport.netty.common;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.kin.kinrpc.transport.netty.AbstractSession;

/**
 * Created by huangjianqin on 2019/6/3.
 */
public class ProtocolConstants {
    private ProtocolConstants(){

    }

    public static final AttributeKey<AbstractSession> SESSION_KEY = AttributeKey.valueOf("session");

    public static AbstractSession session(Channel channel) {
        Attribute<AbstractSession> attr = channel.attr(ProtocolConstants.SESSION_KEY);
        return attr.get();
    }
}
