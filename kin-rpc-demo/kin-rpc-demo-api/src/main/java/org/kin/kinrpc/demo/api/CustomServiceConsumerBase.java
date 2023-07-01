package org.kin.kinrpc.demo.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.transport.cmd.BytebufUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class CustomServiceConsumerBase {
    protected static void invokeCustomService(CustomService customService) {
        invoke("findAll", () -> System.out.println(customService.findAll()));
        invoke("find", () -> System.out.println(customService.find("A", 1)));
        invoke("find", () -> System.out.println(customService.find("A", 11)));
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(1);
            System.out.println(customService.find2(byteBuf));
        });
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(10);
            System.out.println(customService.find2(byteBuf));
        });
        invoke("exists", () -> System.out.println(customService.exists("B", 2)));
        invoke("exists", () -> System.out.println(customService.exists("B", 21)));
        invoke("concat", () -> System.out.println(customService.concat("prefix-", "abc".getBytes(StandardCharsets.UTF_8))));
        invoke("throwException", customService::throwException);
        invoke("delayRandom", () -> System.out.println(customService.delayRandom()));
        asyncInvoke(customService.asyncFind("A", 1), "asyncFind", System.out::println);
        asyncInvoke(customService.asyncFind("A", 11), "asyncFind", System.out::println);
        asyncInvoke(customService.reactiveFind("A", 1).toFuture(), "reactiveFind", System.out::println);
        asyncInvoke(customService.reactiveFind("A", 11).toFuture(), "reactiveFind", System.out::println);
        invoke("findWithAsyncContext", () -> System.out.println(customService.findWithAsyncContext("A", 1)));
        invoke("findWithAsyncContext", () -> System.out.println(customService.findWithAsyncContext("A", 11)));
    }

    protected static void invokeGenericCustomService(GenericService genericCustomService) {
        invoke("findAll", () -> System.out.println(genericCustomService.invoke("findAll", List.class)));
        invoke("find", () -> System.out.println(genericCustomService.invoke("find", User.class, "A", 1)));
        invoke("find", () -> System.out.println(genericCustomService.invoke("find", User.class, "A", 11)));
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(1);
            System.out.println(genericCustomService.invoke("find2", User.class, byteBuf));
        });
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(10);
            System.out.println(genericCustomService.invoke("find2", User.class, byteBuf));
        });
        invoke("exists", () -> System.out.println(genericCustomService.invoke("exists", Boolean.class, "B", 2)));
        invoke("exists", () -> System.out.println(genericCustomService.invoke("exists", Boolean.class, "B", 21)));
        invoke("concat", () -> System.out.println(genericCustomService.invoke("concat", String.class, "prefix-", "abc".getBytes(StandardCharsets.UTF_8))));
        invoke("throwException", () -> genericCustomService.invoke("throwException"));
        invoke("delayRandom", () -> System.out.println(genericCustomService.invoke("delayRandom", Integer.class)));
        asyncInvoke(genericCustomService.asyncInvoke("asyncFind", User.class, "A", 1), "asyncFind", System.out::println);
        asyncInvoke(genericCustomService.asyncInvoke("asyncFind", User.class, "A", 11), "asyncFind", System.out::println);
        asyncInvoke(genericCustomService.reactiveInvoke("asyncFind", User.class, "A", 1).toFuture(), "reactiveFind", System.out::println);
        asyncInvoke(genericCustomService.reactiveInvoke("asyncFind", User.class, "A", 11).toFuture(), "reactiveFind", System.out::println);
        invoke("findWithAsyncContext", () -> System.out.println(genericCustomService.invoke("findWithAsyncContext", User.class, "A", 1)));
        invoke("findWithAsyncContext", () -> System.out.println(genericCustomService.invoke("findWithAsyncContext", User.class, "A", 11)));
    }

    private static void invoke(String message, Runnable runnable) {
        try {
            System.out.println(message);
            runnable.run();
            System.out.println();
        } catch (Exception e) {
            //异常则输出, 然后继续执行下一task
            e.printStackTrace();
        }
    }

    private static <T> void asyncInvoke(CompletableFuture<T> future,
                                        String message,
                                        Consumer<T> consumer) {
        future.whenComplete((r, t) -> {
            if (Objects.isNull(t)) {
                invoke(message, () -> consumer.accept(r));
            } else {
                t.printStackTrace();
            }
        });
    }
}
