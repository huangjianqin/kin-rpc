package org.kin.kinrpc.transport;

import org.kin.framework.collection.CopyOnWriteMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link RequestProcessor}注册中心
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RequestProcessorRegistry {
    /** key -> interest, value -> {@link RequestProcessor}实例 */
    private final Map<String, RequestProcessor<?>> processorMap = new CopyOnWriteMap<>(() -> new HashMap<>(16));

    /**
     * 注册{@link RequestProcessor}实例
     * @param processor {@link RequestProcessor}实例
     */
    public void register(RequestProcessor<?> processor){
        String interest = processor.interest();
        if (processorMap.containsKey(interest)) {
            throw new RemotingException(String.format("processor with interest '%s' has been registered", interest));
        }

        processorMap.put(interest, processor);
    }

    /**
     * 根据interest返回对应的{@link RequestProcessor}实例
     * @param interest request processor interest
     * @return  {@link RequestProcessor}实例
     */
    public RequestProcessor<?> getByInterest(String interest){
        RequestProcessor<?> processor = processorMap.get(interest);
        if (Objects.isNull(processor)) {
            throw new RemotingException(String.format("processor with interest '%s' is not exists", interest));
        }

        return processor;
    }
}
