package org.kin.kinrpc.boot.observability.autoconfigure;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * register observationHandlers to observationConfig
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public class ObservationRegistryPostProcessor implements BeanPostProcessor {
    /** {@link ObservationHandlerGrouping}实例列表 */
    private final ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping;
    /** {@link ObservationHandler}实例列表 */
    private final ObjectProvider<ObservationHandler<?>> observationHandlers;

    public ObservationRegistryPostProcessor(ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
                                            ObjectProvider<ObservationHandler<?>> observationHandlers) {
        this.observationHandlerGrouping = observationHandlerGrouping;
        this.observationHandlers = observationHandlers;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ObservationRegistry) {
            //将observationHandlers注册到observationConfig
            ObservationRegistry observationRegistry = (ObservationRegistry) bean;
            List<ObservationHandler<?>> observationHandlerList =
                    observationHandlers.orderedStream().collect(Collectors.toList());
            observationHandlerGrouping.ifAvailable(grouping -> grouping.apply(observationHandlerList,
                    observationRegistry.observationConfig()));
        }
        return bean;
    }
}
