package org.kin.kinrpc.demo.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
public class DemoServiceFallback implements DemoService {
    private static final User FALLBACK_USER = User.of("fallback", -1);

    @Override
    public List<User> findAll() {
        return Collections.emptyList();
    }

    @Override
    public User find(String name, int age) {
        return FALLBACK_USER;
    }

    @Override
    public ByteBuf find2(ByteBuf byteBuf) {
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public Boolean exists(String name, int age) {
        return false;
    }

    @Override
    public String concat(String s1, byte[] bytes) {
        return "";
    }

    @Override
    public void runWithError() {

    }

    @Override
    public int delayRandom() {
        return -1;
    }

    @Override
    public int delayRandom2() {
        return -1;
    }

    @Override
    public CompletableFuture<User> asyncFind(String name, int age) {
        return CompletableFuture.completedFuture(FALLBACK_USER);
    }

    @Override
    public User asyncFind2(String name, int age) {
        return FALLBACK_USER;
    }

    @Override
    public Mono<User> reactiveFind(String name, int age) {
        return Mono.just(FALLBACK_USER);
    }

    @Override
    public User findWithAsyncContext(String name, int age) {
        return FALLBACK_USER;
    }

    @Override
    public CompletableFuture<Void> asyncRunWithError() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void printAttachments() {
        System.out.println("attachment not found");
    }

    @Override
    public boolean valid(String name, int age, Parameter p) {
        return false;
    }

    @Override
    public boolean failback() {
        return false;
    }

    @Override
    public boolean forking() {
        return false;
    }

    @Override
    public int broadcast() {
        return -1;
    }
}
