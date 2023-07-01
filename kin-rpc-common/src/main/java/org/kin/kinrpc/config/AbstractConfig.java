package org.kin.kinrpc.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.kin.kinrpc.IllegalConfigException;

/**
 * config抽象父类
 *
 * @author huangjianqin
 * @date 2023/6/30
 */
public abstract class AbstractConfig implements Config {
    /**
     * 检查配置合法性
     */
    protected void checkValid() {
        //default do nothing
    }

    /**
     * 检查配置, 如果{@code expression}为false, 则抛{@link  IllegalConfigException}异常
     *
     * @param expression   配置检查表达式
     * @param errorMessage 配置异常消息
     */
    protected static void check(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalConfigException(String.valueOf(errorMessage));
        }
    }

    /**
     * 缺省配置, 设置默认值
     */
    protected void setUpDefaultConfig() {
        //default do nothing
    }
}
