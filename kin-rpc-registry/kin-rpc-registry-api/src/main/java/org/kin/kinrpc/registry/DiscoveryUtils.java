package org.kin.kinrpc.registry;

import org.kin.kinrpc.ReferenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/7/19
 */
public final class DiscoveryUtils {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryUtils.class);

    private DiscoveryUtils() {
    }

    /**
     * 并发请求, 然后超时等待所有请求返回结果
     *
     * @param suppliers 请求逻辑
     * @param desc      请求描述
     * @return 请求结果
     */
    public static <T> List<T> concurrentSupply(List<Supplier<T>> suppliers, String desc) {
        //default 10s
        return concurrentSupply(suppliers, desc, 10_000);
    }

    /**
     * 并发请求, 然后超时等待所有请求返回结果
     *
     * @param suppliers 请求逻辑
     * @param desc      请求描述
     * @param timeoutMs 超时时间(毫秒)
     * @return 请求结果
     */
    public static <T> List<T> concurrentSupply(List<Supplier<T>> suppliers, String desc, int timeoutMs) {
        List<CompletableFuture<T>> futures = new ArrayList<>();
        for (Supplier<T> supplier : suppliers) {
            futures.add(CompletableFuture.supplyAsync(supplier, ReferenceContext.DISCOVERY_SCHEDULER));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("concurrent {} error", desc, e);
        } catch (TimeoutException e) {
            log.error("concurrent {} timeout", desc, e);
        }

        return futures.stream()
                .filter(CompletableFuture::isDone)
                .filter(f -> !f.isCompletedExceptionally() || !f.isCancelled())
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        log.error("get {} from future error", desc, e.getCause());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
