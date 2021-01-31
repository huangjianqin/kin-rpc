package org.kin.kinrpc.transport.rsocket;

import io.rsocket.Payload;
import io.rsocket.ipc.MutableRouter;
import io.rsocket.ipc.routing.SimpleRouter;
import io.rsocket.ipc.util.IPCChannelFunction;
import io.rsocket.ipc.util.IPCFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支持多线程操作的router
 * 参考{@link SimpleRouter}
 * <p>
 * rsocket rpc自动生成的代码中, route = 服务接口名+方法名
 *
 * @author huangjianqin
 * @date 2021/1/31
 */
class CopyOnWriteRouter implements MutableRouter<CopyOnWriteRouter> {
    /** 已注册的fireAndForget router */
    private Map<String, IPCFunction<Mono<Void>>> fireAndForgetRegistry = new HashMap<>();
    /** 已注册的requestResponse router */
    private Map<String, IPCFunction<Mono<Payload>>> requestResponseRegistry = new HashMap<>();
    /** 已注册的requestStream router */
    private Map<String, IPCFunction<Flux<Payload>>> requestStreamRegistry = new HashMap<>();
    /** 已注册的requestChannel router */
    private Map<String, IPCChannelFunction> requestChannelRegistry = new HashMap<>();

    @Override
    public CopyOnWriteRouter withFireAndForgetRoute(String route, IPCFunction<Mono<Void>> ipcFunction) {
        withFireAndForgetRoute0(route, ipcFunction);
        return this;
    }

    /**
     * 基于copy-on-write, 注册fireAndForget router
     */
    private synchronized void withFireAndForgetRoute0(String route, IPCFunction<Mono<Void>> ipcFunction) {
        Map<String, IPCFunction<Mono<Void>>> fireAndForgetRegistry = new HashMap<>(this.fireAndForgetRegistry);
        fireAndForgetRegistry.put(route, ipcFunction);
        this.fireAndForgetRegistry = Collections.unmodifiableMap(fireAndForgetRegistry);
    }

    /**
     * 根据服务名, 取消注册fireAndForget router
     */
    public synchronized void unregisterFireAndForgetRoute(String service) {
        Map<String, IPCFunction<Mono<Void>>> fireAndForgetRegistry = new HashMap<>(this.fireAndForgetRegistry);
        fireAndForgetRegistry = fireAndForgetRegistry.entrySet().stream()
                .filter(e -> e.getKey().contains(service))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.fireAndForgetRegistry = Collections.unmodifiableMap(fireAndForgetRegistry);
    }

    @Override
    public CopyOnWriteRouter withRequestResponseRoute(String route, IPCFunction<Mono<Payload>> ipcFunction) {
        withRequestResponseRoute0(route, ipcFunction);
        return this;
    }

    /**
     * 基于copy-on-write, 注册RequestResponse router
     */
    private synchronized void withRequestResponseRoute0(String route, IPCFunction<Mono<Payload>> ipcFunction) {
        Map<String, IPCFunction<Mono<Payload>>> requestResponseRegistry = new HashMap<>(this.requestResponseRegistry);
        requestResponseRegistry.put(route, ipcFunction);
        this.requestResponseRegistry = Collections.unmodifiableMap(requestResponseRegistry);
    }

    /**
     * 根据服务名, 取消注册RequestResponse router
     */
    public synchronized void unregisterRequestResponseRoute(String service) {
        Map<String, IPCFunction<Mono<Payload>>> requestResponseRegistry = new HashMap<>(this.requestResponseRegistry);
        requestResponseRegistry = requestResponseRegistry.entrySet().stream()
                .filter(e -> e.getKey().contains(service))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.requestResponseRegistry = Collections.unmodifiableMap(requestResponseRegistry);
    }

    @Override
    public CopyOnWriteRouter withRequestStreamRoute(String route, IPCFunction<Flux<Payload>> ipcFunction) {
        withRequestStreamRoute0(route, ipcFunction);
        return this;
    }

    /**
     * 基于copy-on-write, 注册RequestStream router
     */
    private synchronized void withRequestStreamRoute0(String route, IPCFunction<Flux<Payload>> ipcFunction) {
        Map<String, IPCFunction<Flux<Payload>>> requestStreamRegistry = new HashMap<>(this.requestStreamRegistry);
        requestStreamRegistry.put(route, ipcFunction);
        this.requestStreamRegistry = Collections.unmodifiableMap(requestStreamRegistry);
    }

    /**
     * 根据服务名, 取消注册RequestStream router
     */
    public synchronized void unregisterRequestStreamRoute(String service) {
        Map<String, IPCFunction<Flux<Payload>>> requestStreamRegistry = new HashMap<>(this.requestStreamRegistry);
        requestStreamRegistry = requestStreamRegistry.entrySet().stream()
                .filter(e -> e.getKey().contains(service))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.requestStreamRegistry = Collections.unmodifiableMap(requestStreamRegistry);
    }

    @Override
    public CopyOnWriteRouter withRequestChannelRoute(String route, IPCChannelFunction ipcChannelFunction) {
        withRequestChannelRoute0(route, ipcChannelFunction);
        return this;
    }

    /**
     * 基于copy-on-write, 注册RequestChannel router
     */
    private synchronized void withRequestChannelRoute0(String route, IPCChannelFunction ipcChannelFunction) {
        Map<String, IPCChannelFunction> requestChannelRegistry = new HashMap<>(this.requestChannelRegistry);
        requestChannelRegistry.put(route, ipcChannelFunction);
        this.requestChannelRegistry = Collections.unmodifiableMap(requestChannelRegistry);
    }

    /**
     * 根据服务名, 取消注册RequestChannel router
     */
    public synchronized void unregisterRequestChannelRoute(String service) {
        Map<String, IPCChannelFunction> requestChannelRegistry = new HashMap<>(this.requestChannelRegistry);
        requestChannelRegistry = requestChannelRegistry.entrySet().stream()
                .filter(e -> e.getKey().contains(service))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.requestChannelRegistry = Collections.unmodifiableMap(requestChannelRegistry);
    }

    @Override
    public IPCFunction<Mono<Void>> routeFireAndForget(String route) {
        return fireAndForgetRegistry.get(route);
    }

    @Override
    public IPCFunction<Mono<Payload>> routeRequestResponse(String route) {
        return requestResponseRegistry.get(route);
    }

    @Override
    public IPCFunction<Flux<Payload>> routeRequestStream(String route) {
        return requestStreamRegistry.get(route);
    }

    @Override
    public IPCChannelFunction routeRequestChannel(String route) {
        return requestChannelRegistry.get(route);
    }

    /**
     * @return 是否没有任何路由
     */
    public synchronized boolean isEmpty() {
        return fireAndForgetRegistry.isEmpty() &&
                requestResponseRegistry.isEmpty() &&
                requestStreamRegistry.isEmpty() &&
                requestChannelRegistry.isEmpty();
    }
}