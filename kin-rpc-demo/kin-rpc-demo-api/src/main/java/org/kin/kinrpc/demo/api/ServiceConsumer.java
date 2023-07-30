package org.kin.kinrpc.demo.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.RpcContext;
import org.kin.kinrpc.transport.cmd.BytebufUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class ServiceConsumer {
    private static final int DELAY_RANDOM_TIMES = 5;

    protected static void invokeDemoService(DemoService demoService) {
        invoke("findAll", demoService::findAll);
        invoke("find", () -> demoService.find("A", 1));
        invoke("find", () -> demoService.find("A", 11));
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(1);
            return readUserByteBuf(demoService.find2(byteBuf));
        });
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(10);
            return readUserByteBuf(demoService.find2(byteBuf));

        });
        invoke("exists", () -> demoService.exists("B", 2));
        invoke("exists", () -> demoService.exists("B", 21));
        invoke("concat", () -> demoService.concat("prefix-", "abc".getBytes(StandardCharsets.UTF_8)));
        invoke("runWithError", () -> {
            demoService.runWithError();
            return "";
        });
        for (int i = 0; i < DELAY_RANDOM_TIMES; i++) {
            invoke("delayRandom", demoService::delayRandom);
        }
        asyncInvoke(demoService.asyncFind("A", 1), "asyncFind");
        asyncInvoke(demoService.asyncFind("A", 11), "asyncFind");
        asyncInvoke(demoService.reactiveFind("A", 1).toFuture(), "reactiveFind");
        asyncInvoke(demoService.reactiveFind("A", 11).toFuture(), "reactiveFind");
        invoke("findWithAsyncContext", () -> demoService.findWithAsyncContext("A", 1));
        invoke("findWithAsyncContext", () -> demoService.findWithAsyncContext("A", 11));
        asyncInvoke(demoService.asyncRunWithError(), "asyncRunWithError");
        asyncInvoke(() -> demoService.asyncFind2("A", 1), "asyncFind2");
        asyncInvoke(() -> demoService.asyncFind2("A", 11), "asyncFind2");
        RpcContext.attach("A", "1");
        RpcContext.attach("B", "1");
        invoke("printAttachments", () -> {
            demoService.printAttachments();
            return "";
        });
        RpcContext.attach("B", "2");
        RpcContext.attach("C", "2");
        invoke("printAttachments", () -> {
            demoService.printAttachments();
            return "";
        });
        invoke("delayRandom2", demoService::delayRandom2);
        try {
            int sleep = ThreadLocalRandom.current().nextInt(2500);
            System.out.println(String.format("sleep %dms", sleep));
            Thread.sleep(sleep);
        } catch (InterruptedException e) {

        }
        invoke("delayRandom2", demoService::delayRandom2);
        try {
            int sleep = ThreadLocalRandom.current().nextInt(2500);
            System.out.println(String.format("sleep %dms", sleep));
            Thread.sleep(sleep);
        } catch (InterruptedException e) {

        }
        invoke("delayRandom2", demoService::delayRandom2);
        try {
            System.out.println("sleep 2000ms");
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }
        invoke("delayRandom2", demoService::delayRandom2);
        invoke("valid", () -> demoService.valid("A", 1, new Parameter("room", 123)));
        invoke("failback", demoService::failback);
        invoke("forking", demoService::forking);
        invoke("broadcast", demoService::broadcast);

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {

        }
    }

    protected static void invokeGenericDemoService(GenericService genericDemoService) {
        invoke("findAll", () -> genericDemoService.invoke("findAll", List.class));
        invoke("find", () -> genericDemoService.invoke("find", User.class, "A", 1));
        invoke("find", () -> genericDemoService.invoke("find", User.class, "A", 11));
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(1);
            return readUserByteBuf(genericDemoService.invoke("find2", ByteBuf.class, byteBuf));
        });
        invoke("find2", () -> {
            ByteBuf byteBuf = Unpooled.buffer();
            BytebufUtils.writeShortString(byteBuf, "A");
            byteBuf.writeByte(10);
            return readUserByteBuf(genericDemoService.invoke("find2", ByteBuf.class, byteBuf));
        });
        invoke("exists", () -> genericDemoService.invoke("exists", Boolean.class, "B", 2));
        invoke("exists", () -> genericDemoService.invoke("exists", Boolean.class, "B", 21));
        invoke("concat", () -> genericDemoService.invoke("concat", String.class, "prefix-", "abc".getBytes(StandardCharsets.UTF_8)));
        invoke("runWithError", () -> {
            genericDemoService.invoke("runWithError");
            return "";
        });
        for (int i = 0; i < DELAY_RANDOM_TIMES; i++) {
            invoke("delayRandom", () -> genericDemoService.invoke("delayRandom", Integer.class));
        }
        asyncInvoke(genericDemoService.asyncInvoke("asyncFind", User.class, "A", 1), "asyncFind");
        asyncInvoke(genericDemoService.asyncInvoke("asyncFind", User.class, "A", 11), "asyncFind");
        asyncInvoke(genericDemoService.reactiveInvoke("asyncFind", User.class, "A", 1).toFuture(), "reactiveFind");
        asyncInvoke(genericDemoService.reactiveInvoke("asyncFind", User.class, "A", 11).toFuture(), "reactiveFind");
        invoke("findWithAsyncContext", () -> genericDemoService.invoke("findWithAsyncContext", User.class, "A", 1));
        invoke("findWithAsyncContext", () -> genericDemoService.invoke("findWithAsyncContext", User.class, "A", 11));
        asyncInvoke(genericDemoService.asyncInvoke("asyncRunWithError"), "asyncRunWithError");
        asyncInvoke(() -> genericDemoService.invoke("asyncFind2", User.class, "A", 1), "asyncFind2");
        asyncInvoke(() -> genericDemoService.invoke("asyncFind2", User.class, "A", 11), "asyncFind2");
        RpcContext.attach("A", "3");
        RpcContext.attach("B", "3");
        invoke("printAttachments", () -> {
            genericDemoService.invoke("printAttachments");
            return "";
        });
        RpcContext.attach("B", "4");
        RpcContext.attach("C", "4");
        invoke("printAttachments", () -> {
            genericDemoService.invoke("printAttachments");
            return "";
        });
        invoke("delayRandom2", () -> genericDemoService.invoke("delayRandom2", Integer.class));
        try {
            int sleep = ThreadLocalRandom.current().nextInt(2500);
            System.out.println(String.format("sleep %dms", sleep));
            Thread.sleep(sleep);
        } catch (InterruptedException e) {

        }
        invoke("delayRandom2", () -> genericDemoService.invoke("delayRandom2", Integer.class));
        try {
            int sleep = ThreadLocalRandom.current().nextInt(2500);
            System.out.println(String.format("sleep %dms", sleep));
            Thread.sleep(sleep);
        } catch (InterruptedException e) {

        }
        invoke("delayRandom2", () -> genericDemoService.invoke("delayRandom2", Integer.class));
        try {
            System.out.println("sleep 2000ms");
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }
        invoke("delayRandom2", () -> genericDemoService.invoke("delayRandom2", Integer.class));
        invoke("valid", () -> genericDemoService.invoke("valid", Boolean.class, "A", 1, new Parameter("room", 123)));
        invoke("failback", () -> genericDemoService.invoke("failback", Boolean.class));
        invoke("forking", () -> genericDemoService.invoke("forking", Boolean.class));
        invoke("broadcast", () -> genericDemoService.invoke("broadcast", Integer.class));

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {

        }
    }

    private static void invoke(String message, Callable<Object> callable) {
        StringBuilder sb = new StringBuilder();
        sb.append("---" + Thread.currentThread().getName() + "---" + message + System.lineSeparator());
        try {
            sb.append(callable.call());
            sb.append(System.lineSeparator());
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

    private static void asyncInvoke(Runnable task,
                                    String message) {
        try {
            task.run();
            Object ret = RpcContext.future().get();
            invoke(message, ret::toString);
        } catch (Exception e) {
            invoke(message, () -> ExceptionUtils.getExceptionDesc(e));
        }
    }

    private static User readUserByteBuf(ByteBuf byteBuf) {
        if (Objects.isNull(byteBuf)) {
            return null;
        }

        String name = BytebufUtils.readShortString(byteBuf);
        byte age = byteBuf.readByte();
        return User.of(name, age);
    }
}
