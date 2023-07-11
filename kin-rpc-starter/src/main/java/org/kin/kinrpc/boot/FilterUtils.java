package org.kin.kinrpc.boot;

import org.kin.kinrpc.Filter;
import org.kin.kinrpc.IllegalConfigException;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author huangjianqin
 * @date 2023/7/11
 */
public final class FilterUtils {

    private FilterUtils() {
    }

    /**
     * 配置filter
     *
     * @param applicationContext    spring application context
     * @param filterBeanNamesGetter 返回filter bean names func
     * @param filterAdder           add filter func
     */
    public static void addFilters(ApplicationContext applicationContext,
                                  Supplier<List<String>> filterBeanNamesGetter,
                                  Consumer<Filter> filterAdder) {
        for (String filterBeanName : filterBeanNamesGetter.get()) {
            Object bean = applicationContext.getBean(filterBeanName);
            if (!(bean instanceof Filter)) {
                throw new IllegalConfigException(String.format("bean '%s' is not a Filter", filterBeanName));
            }

            filterAdder.accept((Filter) bean);
        }
    }
}
