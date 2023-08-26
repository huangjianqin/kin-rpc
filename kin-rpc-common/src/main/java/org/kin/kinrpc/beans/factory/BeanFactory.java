package org.kin.kinrpc.beans.factory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public interface BeanFactory {
    /**
     * 注册bean
     *
     * @param bean bean instance
     */
    void registerBean(Object bean);

    /**
     * 注册bean
     *
     * @param name bean name
     * @param bean bean instance
     */
    void registerBean(String name, Object bean);

    /**
     * 返回{@code type}匹配的所有bean
     *
     * @param type bean class
     * @return {@code type}匹配的所有bean
     */
    <T> List<T> getBeansOfType(Class<T> type);

    /**
     * 返回{@code type}匹配的bean
     *
     * @param type bean class
     * @return {@code type}匹配的bean
     */
    @Nullable
    <T> T getBean(Class<T> type);

    /**
     * 返回{@code type}匹配的bean
     *
     * @param name bean name
     * @param type bean class
     * @return {@code type}匹配的bean
     */
    @Nullable
    <T> T getBean(String name, Class<T> type);
}
