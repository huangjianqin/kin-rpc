package org.kin.kinrpc.demo.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.kin.kinrpc.AsyncContext;
import org.kin.kinrpc.RpcContext;
import org.kin.kinrpc.transport.cmd.BytebufUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2023/7/1
 */
public class DemoServiceImpl implements DemoService {
    private static final List<User> USERS = Arrays.asList(
            User.of("A", 1),
            User.of("B", 2),
            User.of("C", 3),
            User.of("C", 30),
            User.of("C", 31),
            User.of("C", 32),
            User.of("D", 4),
            User.of("E", 5)
    );

    protected int getRpcPort() {
        return -1;
    }

    @Override
    public List<User> findAll() {
        return USERS;
    }

//    @Override
//    public List<User> find(String name) {
//        return USERS.stream()
//                .filter(u -> u.getName()
//                        .equals(name))
//                .collect(Collectors.toList());
//    }

    @Override
    public User find(String name, int age) {
        return USERS.stream()
                .filter(u -> u.getName()
                        .equals(name) && u.getAge() == age)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("can not find any user"));
    }

    @Override
    public ByteBuf find2(ByteBuf byteBuf) {
        try {
            String name = BytebufUtils.readShortString(byteBuf);
            User user = find(name, byteBuf.readByte());

            ByteBuf buffer = Unpooled.buffer();
            BytebufUtils.writeShortString(buffer, user.getName());
            buffer.writeByte(user.getAge());
            return buffer;
        } finally {
            ReferenceCountUtil.safeRelease(byteBuf);
        }
    }

    @Override
    public Boolean exists(String name, int age) {
        User user = USERS.stream()
                .filter(u -> u.getName()
                        .equals(name) && u.getAge() == age)
                .findAny()
                .orElse(null);
        return Objects.nonNull(user);
    }

    @Override
    public String concat(String s1, byte[] bytes) {
        return s1 + new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void runWithError() {
        throw new BusinessException("business error");
    }

    @Override
    public int delayRandom() {
        int delay = 2000 + ThreadLocalRandom.current().nextInt(2_000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(delay);
        return delay;
    }

    @Override
    public int delayRandom2() {
        int delay = 500 + ThreadLocalRandom.current().nextInt(1_000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return delay;
    }

    @Override
    public CompletableFuture<User> asyncFind(String name, int age) {
        CompletableFuture<User> future = new CompletableFuture<>();
        ForkJoinPool.commonPool().execute(() -> {
            int delay = ThreadLocalRandom.current().nextInt(3_000);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                future.complete(find(name, age));
            } catch (Exception e) {
                future.completeExceptionally(new BusinessException(e));
            }
        });
        return future;
    }

    @Override
    public User asyncFind2(String name, int age) {
        return find(name, age);
    }

    @Override
    public Mono<User> reactiveFind(String name, int age) {
        return Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextLong(3_000)))
                .flatMap(t -> {
                    try {
                        return Mono.just(find(name, age));
                    } catch (Exception e) {
                        return Mono.error(new BusinessException(e));
                    }
                });
    }

    @Override
    public User findWithAsyncContext(String name, int age) {
        AsyncContext asyncContext = AsyncContext.start();
        ForkJoinPool.commonPool().execute(() -> {
            int delay = ThreadLocalRandom.current().nextInt(3_000);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                asyncContext.complete(find(name, age));
            } catch (Exception e) {
                asyncContext.completeExceptionally(new BusinessException(e));
            }
        });
        return null;
    }

    @Override
    public CompletableFuture<Void> asyncRunWithError() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException());
        return future;
    }

    @Override
    public void printAttachments() {
        System.out.println(RpcContext.attachments());
    }

    @Override
    public boolean valid(String name, int age, Parameter p) {
        return true;
    }

    @Override
    public boolean failback() {
        if (ThreadLocalRandom.current().nextBoolean()) {
            return true;
        }
        throw new IllegalStateException("failback() run error");
    }

    @Override
    public boolean forking() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
        } catch (InterruptedException e) {
            //ignore
        }
        System.out.println("forking");
        return true;
    }

    @Override
    public int broadcast() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
        } catch (InterruptedException e) {
            //ignore
        }
        System.out.println("broadcast");
        return getRpcPort();
    }
}
