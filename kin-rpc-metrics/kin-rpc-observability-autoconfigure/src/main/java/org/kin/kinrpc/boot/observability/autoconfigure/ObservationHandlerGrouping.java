package org.kin.kinrpc.boot.observability.autoconfigure;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

/**
 * {@link ObservationHandler}类别分组
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
@SuppressWarnings("rawtypes")
class ObservationHandlerGrouping {
    /** ObservationHandler */
    private final List<Class<? extends ObservationHandler>> categories;

    ObservationHandlerGrouping(Class<? extends ObservationHandler> category) {
        this(Collections.singletonList(category));
    }

    ObservationHandlerGrouping(List<Class<? extends ObservationHandler>> categories) {
        this.categories = categories;
    }

    /**
     * 按已注册的分类对{@code handlers}进行分组, 并注册到{@code config}
     *
     * @param handlers {@link ObservationHandler}实例列表
     * @param config   observation配置
     */
    void apply(List<ObservationHandler<?>> handlers, ObservationRegistry.ObservationConfig config) {
        MultiValueMap<Class<? extends ObservationHandler>, ObservationHandler<?>> groupings = new LinkedMultiValueMap<>();
        for (ObservationHandler<?> handler : handlers) {
            Class<? extends ObservationHandler> category = findCategory(handler);
            if (category != null) {
                groupings.add(category, handler);
            } else {
                //没有注册类别
                config.observationHandler(handler);
            }
        }

        //按类别分组注册observationHandler
        for (Class<? extends ObservationHandler> category : this.categories) {
            List<ObservationHandler<?>> handlerGroup = groupings.get(category);
            if (!CollectionUtils.isEmpty(handlerGroup)) {
                config.observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(handlerGroup));
            }
        }
    }

    /**
     * 依据{@link ObservationHandler}类型寻找分组
     *
     * @param handler {@link ObservationHandler}实例
     * @return {@link ObservationHandler}类别分组
     */
    private Class<? extends ObservationHandler> findCategory(ObservationHandler<?> handler) {
        for (Class<? extends ObservationHandler> category : this.categories) {
            if (category.isInstance(handler)) {
                return category;
            }
        }
        return null;
    }
}
