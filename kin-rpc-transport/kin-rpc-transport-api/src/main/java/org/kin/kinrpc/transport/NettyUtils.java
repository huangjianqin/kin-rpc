package org.kin.kinrpc.transport;

import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020/12/12
 */
public class NettyUtils {
    private NettyUtils() {
    }

    /**
     * 将url attachment的netty配置转换成Map<ChannelOption, Object>
     */
    public static Map<ChannelOption, Object> convert(Url url) {
        //目前就只有9个参数
        Map<ChannelOption, Object> options = new HashMap<>(9);
        if (url.containsParam(Constants.NETTY_CONNECT_TIMEOUT_KEY)) {
            options.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, url.getIntParam(Constants.NETTY_CONNECT_TIMEOUT_KEY));
        }

        if (url.containsParam(Constants.NETTY_NODELAY_KEY)) {
            options.put(ChannelOption.TCP_NODELAY, url.getBooleanParam(Constants.NETTY_NODELAY_KEY));
        }

        if (url.containsParam(Constants.NETTY_KEEPALIVE_KEY)) {
            options.put(ChannelOption.SO_KEEPALIVE, url.getBooleanParam(Constants.NETTY_KEEPALIVE_KEY));
        }

        if (url.containsParam(Constants.NETTY_RCVBUF_KEY)) {
            options.put(ChannelOption.SO_RCVBUF, url.getIntParam(Constants.NETTY_RCVBUF_KEY));
        }

        if (url.containsParam(Constants.NETTY_SNDBUF_KEY)) {
            options.put(ChannelOption.SO_SNDBUF, url.getIntParam(Constants.NETTY_SNDBUF_KEY));
        }

        if (url.containsParam(Constants.NETTY_BACKLOG_KEY)) {
            options.put(ChannelOption.SO_BACKLOG, url.getIntParam(Constants.NETTY_BACKLOG_KEY));
        }

        if (url.containsParam(Constants.NETTY_REUSEADDR_KEY)) {
            options.put(ChannelOption.SO_REUSEADDR, url.getBooleanParam(Constants.NETTY_REUSEADDR_KEY));
        }

        if (url.containsParam(Constants.NETTY_LINGER_KEY)) {
            options.put(ChannelOption.SO_LINGER, url.getIntParam(Constants.NETTY_LINGER_KEY));
        }

        if (url.containsParam(Constants.NETTY_WRITE_BUFF_WATER_MARK_KEY)) {
            String[] splits = url.getParam(Constants.NETTY_WRITE_BUFF_WATER_MARK_KEY).split(",");
            options.put(ChannelOption.WRITE_BUFFER_WATER_MARK,
                    new WriteBufferWaterMark(Integer.parseInt(splits[0]), Integer.parseInt(splits[1])));
        }
        return options;
    }
}
