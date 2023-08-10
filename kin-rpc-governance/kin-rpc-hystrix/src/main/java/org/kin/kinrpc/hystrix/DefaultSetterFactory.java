package org.kin.kinrpc.hystrix;

import com.netflix.hystrix.*;
import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.InvocationConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/8/9
 */
public class DefaultSetterFactory implements SetterFactory {
    /** 单例 */
    private static final DefaultSetterFactory INSTANCE = new DefaultSetterFactory();

    public static SetterFactory instance() {
        return INSTANCE;
    }

    /** hystrix配置名-consumer资源名前缀 */
    private static final String CONSUMER_RES_NAME_PREFIX_KEY = "hystrix.resource.consumer.prefix";
    /** hystrix配置名-服务唯一标识是否带上group和version */
    private static final String GSV_ENABLED = "hystrix.gsv.enabled";
    /** key -> handler唯一id, value -> {@link HystrixCommand.Setter}实例缓存 */
    private final Map<Integer, HystrixCommand.Setter> setterCache = new CopyOnWriteMap<>(() -> new HashMap<>(16));

    private DefaultSetterFactory() {
    }

    @Override
    public HystrixCommand.Setter createSetter(Invocation invocation) {
        int handlerId = invocation.handlerId();
        return setterCache.computeIfAbsent(handlerId, k -> getOrCreateCmdSetter(invocation));
    }

    /**
     * 根据rpc call info创建{@link com.netflix.hystrix.HystrixCommand.Setter}实例
     *
     * @param invocation rpc call info
     * @return {@link com.netflix.hystrix.HystrixCommand.Setter}实例
     */
    private static HystrixCommand.Setter getOrCreateCmdSetter(Invocation invocation) {
        AttachmentMap attachmentMap = new AttachmentMap();
        MethodConfig methodConfig = invocation.attachment(InvocationConstants.METHOD_CONFIG_KEY);
        if (Objects.nonNull(methodConfig)) {
            attachmentMap = new AttachmentMap(methodConfig.attachments());
        }

        String prefix = attachmentMap.attachment(CONSUMER_RES_NAME_PREFIX_KEY, "");
        boolean gsvEnable = attachmentMap.attachment(GSV_ENABLED, false);
        HystrixCommand.Setter commandSetter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(getInterfaceResName(invocation, prefix, gsvEnable)))
                .andCommandKey(HystrixCommandKey.Factory.asKey(getMethodResName(invocation, prefix, gsvEnable)))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(getInterfaceResName(invocation, prefix, gsvEnable)));

        //command properties
        HystrixCommandProperties.Setter commandPropsSetter = HystrixCommandProperties.Setter();
        HystrixThreadPoolProperties.Setter threadPoolPropsSetter = HystrixThreadPoolProperties.Setter();

        for (Map.Entry<String, Object> entry : attachmentMap.attachments().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("hystrix.command.")) {
                key = key.replace("hystrix.command.", "");
                String[] keySplits = key.split("\\.");
                String propSetterName = "with";
                for (String split : keySplits) {
                    propSetterName += StringUtils.firstUpperCase(split);
                }
                setCommandProp(commandPropsSetter, propSetterName, value.toString());
            } else if (key.startsWith("hystrix.threadpool.")) {
                key = key.replace("hystrix.threadpool.", "");
                String[] keySplits = key.split("\\.");
                String propSetterName = "with";
                for (String split : keySplits) {
                    propSetterName += StringUtils.firstUpperCase(split);
                }
                setThreadPoolProp(threadPoolPropsSetter, propSetterName, value.toString());
            }
        }

        //update
        commandSetter.andCommandPropertiesDefaults(commandPropsSetter)
                .andThreadPoolPropertiesDefaults(threadPoolPropsSetter);
        return commandSetter;
    }

    /**
     * 在{@link HystrixCommandProperties.Setter}类中寻找名为{@code propSetterName}的方法, 并以{@code value}为入参调用该方法
     *
     * @param commandPropsSetter command properties setter
     * @param propSetterName     command property setter method name
     * @param value              command property value
     */
    private static void setCommandProp(HystrixCommandProperties.Setter commandPropsSetter,
                                       String propSetterName, String value) {
        Class<HystrixCommandProperties.Setter> commandPropsSetterClass = HystrixCommandProperties.Setter.class;
        for (Method declaredMethod : commandPropsSetterClass.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(propSetterName)) {
                try {
                    declaredMethod.setAccessible(true);
                    declaredMethod.invoke(commandPropsSetter,
                            ClassUtils.convertStr2PrimitiveObj(declaredMethod.getParameterTypes()[0], value));
                    break;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 在{@link HystrixThreadPoolProperties.Setter}类中寻找名为{@code propSetterName}的方法, 并以{@code value}为入参调用该方法
     *
     * @param threadPoolPropsSetter threadPool properties setter
     * @param propSetterName        threadPool property setter method name
     * @param value                 threadPool property value
     */
    private static void setThreadPoolProp(HystrixThreadPoolProperties.Setter threadPoolPropsSetter,
                                          String propSetterName, String value) {
        Class<HystrixThreadPoolProperties.Setter> threadPoolPropsSetterClass = HystrixThreadPoolProperties.Setter.class;
        for (Method declaredMethod : threadPoolPropsSetterClass.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(propSetterName)) {
                try {
                    declaredMethod.setAccessible(true);
                    declaredMethod.invoke(threadPoolPropsSetter, value);
                    break;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 返回服务方法资源名
     *
     * @param invocation         kinrpc invocation
     * @param useGroupAndVersion 服务唯一标识是否带上group和version
     * @return 服务方法资源名
     */
    private static String getMethodResName(Invocation invocation, boolean useGroupAndVersion) {
        StringBuilder buf = new StringBuilder(64);
        String interfaceResource = useGroupAndVersion ? invocation.service() : invocation.serviceName();
        buf.append(interfaceResource)
                .append(":")
                .append(invocation.handlerName())
                .append("(");
        boolean isFirst = true;
        for (Class<?> clazz : invocation.method().getParameterTypes()) {
            if (!isFirst) {
                buf.append(",");
            }
            buf.append(clazz.getName());
            isFirst = false;
        }
        buf.append(")");
        return buf.toString();
    }

    /**
     * 返回服务方法资源名
     *
     * @param invocation kinrpc invocation
     * @param prefix     资源名前缀
     * @return 服务方法资源名
     */
    private static String getMethodResName(Invocation invocation,
                                           String prefix,
                                           boolean useGroupAndVersion) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }

        return prefix + getMethodResName(invocation, useGroupAndVersion);
    }

    /**
     * 返回服务接口资源名
     *
     * @param invocation kinrpc invocation
     * @param prefix     资源名前缀
     * @return 服务接口资源名
     */
    private static String getInterfaceResName(Invocation invocation,
                                              String prefix,
                                              boolean useGroupAndVersion) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }
        return prefix + (useGroupAndVersion ? invocation.service() : invocation.serviceName());
    }
}
