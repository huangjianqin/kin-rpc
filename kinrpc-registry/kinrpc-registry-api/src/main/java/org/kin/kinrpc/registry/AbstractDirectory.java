package org.kin.kinrpc.registry;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory implements Directory {
    protected static final Logger log = LoggerFactory.getLogger("registry");

    protected final Class<?> interfaceClass;
    protected final int connectTimeout;

    //所有的消费者共用一个EventLoopGroup
    protected final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected AbstractDirectory(Class<?> interfaceClass, int connectTimeout) {
        this.interfaceClass = interfaceClass;
        this.connectTimeout = connectTimeout;
    }

    @Override
    public String getServiceName() {
        return interfaceClass.getName();
    }
}
