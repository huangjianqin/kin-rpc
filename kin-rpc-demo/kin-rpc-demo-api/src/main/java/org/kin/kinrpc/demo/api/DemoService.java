package org.kin.kinrpc.demo.api;

import io.netty.buffer.ByteBuf;
import org.kin.kinrpc.RpcContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2021/4/9
 */
public interface DemoService {
    List<User> findAll();

//    /**
//     * 测试重载检查
//     */
//    List<User> find(String name);

    User find(String name, int age);

    /**
     * 测试参数为{@link ByteBuf}
     */
    ByteBuf find2(ByteBuf byteBuf);

    Boolean exists(String name, int age);

    /**
     * 测试参数为byte[]
     */
    String concat(String s1, byte[] bytes);

    /**
     * 主动抛异常
     */
    void runWithError();

    /**
     * {@link Thread#sleep(long)}
     * 测试超时
     */
    int delayRandom();

    /**
     * 测试返回{@link CompletableFuture}
     */
    CompletableFuture<User> asyncFind(String name, int age);

    /**
     * 测试使用{@link RpcContext#future()}
     */
    User asyncFind2(String name, int age);

    /**
     * 测试返回{@link Mono}
     */
    Mono<User> reactiveFind(String name, int age);

    /**
     * 测试{@link org.kin.kinrpc.AsyncContext}
     */
    User findWithAsyncContext(String name, int age);

    /**
     * 异步主动抛异常
     */
    CompletableFuture<Void> asyncRunWithError();

    void printAttachments();
}
