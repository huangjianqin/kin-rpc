package org.kin.kinrpc.hystrix;

/**
 * @author huangjianqin
 * @date 2023/8/8
 */
public enum AsyncHystrixEvent {
    /** command提交 */
    EMIT,
    /** command执行成功 */
    INVOKE_SUCCESS,
    /** command fallback提交 */
    FALLBACK_EMIT,
    /** command fallback解锁 */
    FALLBACK_UNLOCKED,
    /** command fallback执行成功 */
    FALLBACK_SUCCESS,
}
