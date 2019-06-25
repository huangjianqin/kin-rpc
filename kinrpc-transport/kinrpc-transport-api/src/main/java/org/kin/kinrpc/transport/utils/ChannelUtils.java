package org.kin.kinrpc.transport.utils;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * Created by huangjianqin on 2019/6/3.
 */
public class ChannelUtils {
    private ChannelUtils(){

    }

    public static String getIP(Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress().toString().substring(1);
    }

    public static long ipHashCode(String ip) {
        String[] splits = ip.split("\\.");
        long hashcode = 0l;
        int offset = 24;
        for (String item : splits) {
            hashcode += Long.valueOf(item) << offset;
            offset -= 8;
        }
        return hashcode;
    }

    public static long ipHashCode(String ip, int port) {
        return ipHashCode(ip) + port;
    }
}
