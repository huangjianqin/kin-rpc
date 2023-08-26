package org.kin.kinrpc.beans.factory;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.beans.ScopeBeanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * kinrpc application内部共享的bean factory
 * <p>
 * !!!目前实现仅仅用于外部注册bean, kinrpc内部能够获取, 暂不支持bean实例化初始化, 也不支持获取extension
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public class DefaultBeanFactory implements BeanFactory {
    private static final Logger log = LoggerFactory.getLogger(DefaultBeanFactory.class);

    /** parent bean factory */
    private final DefaultBeanFactory parent;
    /** 默认生成的bean name计数器 */
    private final ConcurrentHashMap<Class<?>, AtomicInteger> beanNameIdCounterMap = new ConcurrentHashMap<>();
    /** 已注册的bean info */
    private final List<BeanInfo> registeredBeanInfos = new CopyOnWriteArrayList<>();

    public DefaultBeanFactory(DefaultBeanFactory parent) {
        this.parent = parent;
    }

    @Override
    public void registerBean(Object bean) {
        this.registerBean(null, bean);
    }

    @Override
    public void registerBean(String name, Object bean) {
        if (containsBean(name, bean)) {
            return;
        }

        Class<?> beanClass = bean.getClass();
        if (name == null) {
            //默认bean name
            name = beanClass.getName() + "#" + getNextId(beanClass);
        }

        registeredBeanInfos.add(new BeanInfo(name, bean));
    }

    /**
     * 返回是否名为{@code name}的{@code bean}已经注册
     *
     * @param name bean name
     * @param bean bean instance
     * @return true表示名为{@code name}的{@code bean}已经注册
     */
    private boolean containsBean(String name, Object bean) {
        for (BeanInfo beanInfo : registeredBeanInfos) {
            if (beanInfo.instance == bean &&
                    (name == null || StringUtils.isEquals(name, beanInfo.name))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成{@code  beanClass}下一次默认bean name id
     *
     * @param beanClass bean class
     * @return {@code  beanClass}下一次默认bean name id
     */
    private int getNextId(Class<?> beanClass) {
        return beanNameIdCounterMap.computeIfAbsent(beanClass, key -> new AtomicInteger()).incrementAndGet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getBeansOfType(Class<T> type) {
        List<T> currentBeans = (List<T>) registeredBeanInfos.stream()
                .filter(beanInfo -> type.isInstance(beanInfo.instance))
                .map(beanInfo -> beanInfo.instance)
                .collect(Collectors.toList());
        if (parent != null) {
            //还有parent bean factory
            currentBeans.addAll(parent.getBeansOfType(type));
        }
        return currentBeans;
    }

    @Nullable
    @Override
    public <T> T getBean(Class<T> type) {
        return this.getBean(null, type);
    }

    @Nullable
    @Override
    public <T> T getBean(String name, Class<T> type) {
        T bean = getBean0(name, type);
        if (bean == null && parent != null) {
            return parent.getBean(name, type);
        }
        return bean;
    }

    /**
     * 返回{@code type}匹配的bean
     *
     * @param name bean name
     * @param type bean class
     * @return {@code type}匹配的bean
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getBean0(String name, Class<T> type) {
        if (type == Object.class) {
            //过滤Object
            return null;
        }

        List<BeanInfo> candidates = null;
        BeanInfo firstCandidate = null;
        for (BeanInfo beanInfo : registeredBeanInfos) {
            if (!type.isAssignableFrom(beanInfo.instance.getClass())) {
                // if required bean type is same class/superclass/interface of the registered bean
                continue;
            }

            if (StringUtils.isEquals(beanInfo.name, name)) {
                return (T) beanInfo.instance;
            } else {
                // optimize for only one matched bean
                if (firstCandidate == null) {
                    firstCandidate = beanInfo;
                } else {
                    if (candidates == null) {
                        candidates = new ArrayList<>();
                        candidates.add(firstCandidate);
                    }
                    candidates.add(beanInfo);
                }
            }
        }

        // if bean name not matched and only single candidate
        if (candidates != null) {
            if (candidates.size() == 1) {
                return (T) candidates.get(0).instance;
            } else if (candidates.size() > 1) {
                List<String> candidateBeanNames = candidates.stream()
                        .map(beanInfo -> beanInfo.name)
                        .collect(Collectors.toList());
                throw new ScopeBeanException("expected single matching bean but found " + candidates.size() + " candidates for type [" + type.getName() + "]: " + candidateBeanNames);
            }
        } else if (firstCandidate != null) {
            return (T) firstCandidate.instance;
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------------------------------

    /**
     * bean信息
     */
    static class BeanInfo {
        /** bean name */
        private final String name;
        /** bean instance */
        private final Object instance;

        BeanInfo(String name, Object instance) {
            this.name = name;
            this.instance = instance;
        }
    }
}
