package org.kin.kinrpc.registry.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.keeper.Keeper;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * redis 注册中心
 *
 * @author huangjianqin
 * @date 2020/8/12
 */
public final class RedisRegistry extends AbstractRegistry implements LoggerOprs {
    private static final String KEY_PREFIX = "kin-rpc-";

    /** 主机名 */
    private final String host;
    /** 端口 */
    private final int port;
    /** 连接会话超时 */
    private final long sessionTimeout;
    /** 轮询redis services key间隔 */
    private final long watchInterval;

    /** redis客户端 */
    private RedisClient client;
    /** redis连接 */
    private StatefulRedisConnection<String, String> connection;
    /** 定时轮询redis services key, 判断对应服务地址是否有变化 */
    private Keeper.KeeperStopper serviceWatcher;
    /** 执行RedisDirectory discover 的worker */
    private ExecutionContext watcherCtx = ExecutionContext.fix(SysUtils.CPU_NUM / 2 + 1, "redis-watcher");

    public RedisRegistry(Url url) {
        super(url);
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        //解析地址
        Object[] addressParseResult = NetUtils.parseIpPort(address);
        this.host = addressParseResult[0].toString();
        this.port = Integer.parseInt(addressParseResult[1].toString());

        this.sessionTimeout = Long.parseLong(url.getParam(Constants.SESSION_TIMEOUT_KEY));
        this.watchInterval = Long.parseLong(url.getParam(Constants.WATCH_INTERVAL_KEY));
    }

    @Override
    public void connect() {
        RedisURI uri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(Duration.of(sessionTimeout, ChronoUnit.MILLIS))
                .build();
        client = RedisClient.create(uri);
        connection = client.connect();

        serviceWatcher = Keeper.keep(this::watch);

        log().info("redis client connect success");
    }

    private void watch() {
        try {
            TimeUnit.MILLISECONDS.sleep(watchInterval - System.currentTimeMillis() % 1000);
        } catch (InterruptedException e) {
            return;
        }

        RedisCommands<String, String> redisCommands = connection.sync();

        for (Map.Entry<String, Directory> entry : directoryCache.asMap().entrySet()) {
            watcherCtx.execute(() -> watch0(redisCommands, entry.getKey(), entry.getValue()));
        }
    }

    private void watch0(RedisCommands<String, String> redisCommands, String serviceName, Directory directory) {
        Set<String> urlStrs = redisCommands.smembers(getServiceKey(serviceName));
        directory.discover(url, urlStrs.stream().map(Url::of).collect(Collectors.toList()));
    }

    /**
     * 获取服务在redis上设置的key
     */
    private String getServiceKey(String serviceName) {
        return KEY_PREFIX.concat(serviceName);
    }

    @Override
    public void register(Url url) {
        String serviceKey = url.getServiceKey();
        log().info("provider register service '{}' ", serviceKey);

        RedisCommands<String, String> redisCommands = connection.sync();
        //利用集合存储服务地址
        redisCommands.sadd(getServiceKey(serviceKey), url.str());
    }

    @Override
    public void unRegister(Url url) {
        String serviceKey = url.getServiceKey();
        log().info("provider unregister service '{}' ", serviceKey);

        RedisCommands<String, String> redisCommands = connection.sync();
        redisCommands.srem(getServiceKey(serviceKey), url.str());
    }

    @Override
    public Directory subscribe(String serviceKey) {
        log().info("reference subscribe service '{}' ", serviceKey);
        Directory directory = new Directory(serviceKey);
        directoryCache.put(serviceKey, directory);
        return directory;
    }

    @Override
    public void unSubscribe(String serviceKey) {
        log().info("reference unsubscribe service '{}' ", serviceKey);
        Directory directory = directoryCache.getIfPresent(serviceKey);
        if (directory != null) {
            directory.destroy();
        }
        directoryCache.invalidate(serviceKey);
    }

    @Override
    public void destroy() {
        serviceWatcher.stop();
        connection.close();
        client.shutdown();

        log().info("redis client connect destroyed");
    }
}
