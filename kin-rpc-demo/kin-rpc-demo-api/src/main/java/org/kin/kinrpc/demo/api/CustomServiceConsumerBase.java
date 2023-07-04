package org.kin.kinrpc.demo.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.transport.cmd.BytebufUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class CustomServiceConsumerBase {
    private static final int DELAY_RANDOM_TIMES = 5;

    protected static void invokeCustomService(CustomService customService) {
        invoke("findAll", customService::findAll);
        invoke("find", () -> customService.find("A", 1));
        invoke("find", () -> customService.find("A", 11));
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(1);
            return customService.find2(byteBuf);
        });
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(10);
            return customService.find2(byteBuf);

        });
        invoke("exists", () -> customService.exists("B", 2));
        invoke("exists", () -> customService.exists("B", 21));
        invoke("concat", () -> customService.concat("prefix-", "abc".getBytes(StandardCharsets.UTF_8)));
        invoke("runWithError", () -> {
            customService.runWithError();
            return "";
        });
        for (int i = 0; i < DELAY_RANDOM_TIMES; i++) {
            invoke("delayRandom", customService::delayRandom);
        }
        asyncInvoke(customService.asyncFind("A", 1), "asyncFind");
        asyncInvoke(customService.asyncFind("A", 11), "asyncFind");
        asyncInvoke(customService.reactiveFind("A", 1).toFuture(), "reactiveFind");
        asyncInvoke(customService.reactiveFind("A", 11).toFuture(), "reactiveFind");
        invoke("findWithAsyncContext", () -> customService.findWithAsyncContext("A", 1));
        invoke("findWithAsyncContext", () -> customService.findWithAsyncContext("A", 11));
        asyncInvoke(customService.asyncRunWithError(), "asyncRunWithError");

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {

        }
    }

    protected static void invokeGenericCustomService(GenericService genericCustomService) {
        invoke("findAll", () -> genericCustomService.invoke("findAll", List.class));
        invoke("find", () -> genericCustomService.invoke("find", User.class, "A", 1));
        invoke("find", () -> genericCustomService.invoke("find", User.class, "A", 11));
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(1);
            return genericCustomService.invoke("find2", User.class, byteBuf);
        });
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(10);
            return genericCustomService.invoke("find2", User.class, byteBuf);
        });
        invoke("exists", () -> genericCustomService.invoke("exists", Boolean.class, "B", 2));
        invoke("exists", () -> genericCustomService.invoke("exists", Boolean.class, "B", 21));
        invoke("concat", () -> genericCustomService.invoke("concat", String.class, "prefix-", "abc".getBytes(StandardCharsets.UTF_8)));
        invoke("throwException", () -> {
            genericCustomService.invoke("runWithError");
            return "";
        });
        for (int i = 0; i < DELAY_RANDOM_TIMES; i++) {
            invoke("delayRandom", () -> genericCustomService.invoke("delayRandom", Integer.class));
        }
        asyncInvoke(genericCustomService.asyncInvoke("asyncFind", User.class, "A", 1), "asyncFind");
        asyncInvoke(genericCustomService.asyncInvoke("asyncFind", User.class, "A", 11), "asyncFind");
        asyncInvoke(genericCustomService.reactiveInvoke("asyncFind", User.class, "A", 1).toFuture(), "reactiveFind");
        asyncInvoke(genericCustomService.reactiveInvoke("asyncFind", User.class, "A", 11).toFuture(), "reactiveFind");
        invoke("findWithAsyncContext", () -> genericCustomService.invoke("findWithAsyncContext", User.class, "A", 1));
        invoke("findWithAsyncContext", () -> genericCustomService.invoke("findWithAsyncContext", User.class, "A", 11));
        asyncInvoke(genericCustomService.asyncInvoke("asyncRunWithError"), "asyncRunWithError");

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {

        }
    }

    private static void invoke(String message, Callable<Object> callable) {
        StringBuilder sb = new StringBuilder();
        sb.append("---" + Thread.currentThread().getName() + "---" + message + System.lineSeparator());
        try {
            sb.append(callable.call().toString() + System.lineSeparator());
        } catch (Exception e) {
            //异常则输出, 然后继续执行下一task
            sb.append(ExceptionUtils.getExceptionDesc(e) + System.lineSeparator());
        }
        sb.append("---" + System.lineSeparator());
        System.out.println(sb);
    }

    private static <T> void asyncInvoke(CompletableFuture<T> future,
                                        String message) {
        future.whenComplete((r, t) -> {
            if (Objects.isNull(t)) {
                invoke(message, r::toString);
            } else {
                invoke(message, () -> ExceptionUtils.getExceptionDesc(t));
            }
        });
    }
}
