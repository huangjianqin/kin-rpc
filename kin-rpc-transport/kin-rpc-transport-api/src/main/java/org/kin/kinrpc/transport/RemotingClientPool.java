package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RequestCommand;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2023/6/29
 */
public class RemotingClientPool<C extends RemotingClient> implements RemotingClient {
    /** remoting client pool */
    private final List<C> clients = new CopyOnWriteArrayList<>();
    /** round robin形式选择哪个client发起rpc request */
    private final AtomicInteger idx = new AtomicInteger();

    public RemotingClientPool() {
    }

    public RemotingClientPool(List<C> clients) {
        this.clients.addAll(clients);
    }

    /**
     * 添加remoting client
     *
     * @param clients remoting client数组
     */
    public void addClients(C... clients) {
        addClients(Arrays.asList(clients));
    }

    /**
     * 添加remoting client
     *
     * @param clients remoting client列表
     */
    public void addClients(Collection<C> clients) {
        this.clients.addAll(clients);
    }

    @Override
    public void connect() {
        throw new UnsupportedOperationException("remoting client pool does not support connect operation");
    }

    @Override
    public boolean isAvailable() {
        for (C client : clients) {
            if (client.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void shutdown() {
        Iterator<C> iterator = clients.iterator();
        while (iterator.hasNext()) {
            C client = iterator.next();
            client.shutdown();
            iterator.remove();
        }
    }

    /**
     * 返回一个可用client
     *
     * @return remoting client
     */
    @Nullable
    private C select() {
        int size = clients.size();
        int counter = 0;
        while (counter < size) {
            int i = idx.incrementAndGet();
            C c = clients.get(i % size);
            if (c.isAvailable()) {
                return c;
            }
            counter++;
        }

        return null;
    }

    @Override
    public <T> CompletableFuture<T> requestResponse(RequestCommand command) {
        C client = select();
        if (Objects.isNull(client)) {
            throw new TransportException("can not find available client");
        }
        return client.requestResponse(command);
    }

    @Override
    public CompletableFuture<Void> fireAndForget(RequestCommand command) {
        C client = select();
        if (Objects.isNull(client)) {
            throw new TransportException("can not find available client");
        }
        return client.fireAndForget(command);
    }
}
