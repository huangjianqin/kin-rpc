package org.kin.kinrpc;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 当服务方法为异步调用时, 向user暴露接口获取future
 *
 * @author huangjianqin
 * @date 2020/11/11
 */
public class RpcContext {
    /** thread local rpc context */
    private static final ThreadLocal<RpcContext> THREAD_LOCAL_RPC_CONTEXT = ThreadLocal.withInitial(RpcContext::new);

    /**
     * 获取async rpc call关联future
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> future() {
        return (CompletableFuture<T>) THREAD_LOCAL_RPC_CONTEXT.get().future;
    }

    /**
     * 更新async rpc call关联future
     */
    public static void updateFuture(CompletableFuture<?> future) {
        RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().future = future;
    }

    /**
     * 批量attach
     *
     * @param attachments attachments
     */
    public static void attachMany(Map<String, String> attachments) {
        RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.putAll(attachments);
    }

    /**
     * attach
     *
     * @param key   attachment key
     * @param value attachment value
     * @return this
     */
    @Nullable
    public static String attach(String key, String value) {
        return RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.put(key, value);
    }

    /**
     * 是否存在attachment key
     *
     * @param key attachment key
     * @return true表示存在attachment key
     */
    public static boolean hasAttachment(String key) {
        return RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.containsKey(key);
    }

    /**
     * 获取attachment value
     *
     * @param key attachment key
     * @return attachment value
     */
    @Nullable
    public static String attachment(String key) {
        return RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.get(key);
    }

    /**
     * 获取attachment value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment value
     * @return attachment value
     */
    public static String attachment(String key, String defaultValue) {
        return RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.getOrDefault(key, defaultValue);
    }

    /**
     * 移除attachment
     *
     * @param key attachment key
     * @return attachment value if exists
     */
    @Nullable
    public static String detach(String key) {
        return RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.remove(key);
    }

    /**
     * 返回所有attachment
     *
     * @return 所有attachment
     */
    public static Map<String, String> attachments() {
        return new HashMap<>(RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment);
    }

    /**
     * 移除所有attachment
     */
    public static void clearAttachments() {
        RpcContext.THREAD_LOCAL_RPC_CONTEXT.get().attachment.clear();
    }

    /** rpc call future */
    private CompletableFuture<?> future;
    /** rpc call attachments, 即发送给server的attachments */
    private Map<String, String> attachment = new HashMap<>();

    private RpcContext() {
    }
}
