package org.kin.kinrpc.transport;

import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
public class Protocols {
    /** key-> class name, value -> Protocol instance */
    private static volatile Map<String, Protocol> PROTOCOL_CACHE = Collections.emptyMap();

    static {
        load();
    }

    private Protocols() {
    }

    private static String getKey(String className) {
        int index = className.indexOf(Protocol.class.getSimpleName());
        if (index > 0) {
            className = className.substring(0, index);
        }

        return className.toLowerCase();
    }

    /**
     * 加载Protocol
     */
    private static void load() {
        Map<String, Protocol> protocolCache = new HashMap<>();
        //加载内部提供的Protocol
        Set<Class<? extends Protocol>> classes = ClassUtils.getSubClass(Protocol.class.getPackage().getName(), Protocol.class, true);
        if (classes.size() > 0) {
            for (Class<? extends Protocol> claxx : classes) {
                String key = getKey(claxx.getSimpleName());
                if (protocolCache.containsKey(key)) {
                    throw new ProtocolConflictException(claxx, protocolCache.get(key).getClass());
                }
                try {
                    Protocol protocol = claxx.newInstance();
                    protocolCache.put(key, protocol);
                } catch (Exception e) {
                    throw new RpcCallErrorException(e);
                }
            }
        }

        //通过spi机制加载自定义的Protocol
        Iterator<Protocol> customProtocols = RpcServiceLoader.LOADER.iterator(Protocol.class);
        while (customProtocols.hasNext()) {
            Protocol protocol = customProtocols.next();
            Class<? extends Protocol> claxx = protocol.getClass();
            String key = getKey(claxx.getSimpleName());
            if (protocolCache.containsKey(key)) {
                throw new ProtocolConflictException(claxx, protocolCache.get(key).getClass());
            }

            protocolCache.put(key, protocol);
        }

        PROTOCOL_CACHE = Collections.unmodifiableMap(protocolCache);
    }

    /**
     * 根据Protocol name 获取Protocol instance
     */
    public static Protocol getProtocol(String type) {
        return PROTOCOL_CACHE.get(type);
    }

    /**
     * 根据Protocol class 返回Protocol name, 如果没有加载到, 则主动重新加载
     */
    public static synchronized String getOrLoadProtocol(Class<? extends Protocol> protocolClass) {
        String key = getKey(protocolClass.getSimpleName());
        if (PROTOCOL_CACHE.containsKey(key)) {
            //已加载
            return key;
        }

        Map<String, Protocol> protocolCache = new HashMap<>(PROTOCOL_CACHE);
        //未加载
        try {
            Protocol protocol = protocolClass.newInstance();
            protocolCache.put(key, protocol);
        } catch (Exception e) {
            throw new RpcCallErrorException(e);
        }

        PROTOCOL_CACHE = Collections.unmodifiableMap(protocolCache);

        return key;
    }
}
