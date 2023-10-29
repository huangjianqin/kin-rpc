package org.kin.kinrpc.validation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.*;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.*;
import javax.validation.groups.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/7/25
 */
@Scope(Scopes.APPLICATION)
public class ValidationFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ValidationFilter.class);
    /** 空{@link Class}数组 */
    private static final Class<?>[] EMPTY_CLASS_ARR = new Class[0];
    /** 单例 */
    private static final ValidationFilter INSTANCE = new ValidationFilter();

    public static ValidationFilter instance() {
        return INSTANCE;
    }

    /** validator */
    private final Validator validator;
    /** 方法参数校验元数据 */
    private final Map<String, MethodValidationMetadata> methodValidationMetadataMap = new CopyOnWriteMap<>(() -> new HashMap<>(16));

    public ValidationFilter() {
        this(Validation.buildDefaultValidatorFactory());
    }

    public ValidationFilter(ValidatorFactory validatorFactory) {
        this(validatorFactory.getValidator());
    }

    public ValidationFilter(Validator validator) {
        this.validator = validator;
    }

    @Override
    public final RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        boolean validation = invocation.boolAttachment(InvocationConstants.VALIDATION_KEY);
        if (!validation) {
            //没有开启服务方法调用参数校验
            return invoker.invoke(invocation);
        }

        Method method = invocation.method();
        if (CollectionUtils.isEmpty(method.getParameters())) {
            //无参
            return invoker.invoke(invocation);
        }

        String key = method.getDeclaringClass().getName() + "$" + method;
        MethodValidationMetadata validationMetadata = methodValidationMetadataMap.computeIfAbsent(key, k -> createMethodValidationMetadata(invocation));

        Class<?>[] groups = validationMetadata.getGroups();

        //校验服务方法中基础类型参数
        String handler = invocation.handler();
        Object[] params = invocation.params();
        Object parameterBean = validationMetadata.createParameterBean(params);
        if (Objects.nonNull(parameterBean)) {
            Set<ConstraintViolation<Object>> violations = validator.validate(parameterBean, groups);
            if (!violations.isEmpty()) {
                return RpcResult.fail(invocation, new ConstraintViolationException(
                        toErrorString("fail to validate rpc call params, handler=%s, due to %s", handler, violations),
                        violations));
            }
        }

        //如果是非基础类型且非基础类型数组, 则进行校验
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            try {
                validate(handler, method.getParameters()[i], param, groups);
            } catch (Exception e) {
                return RpcResult.fail(invocation, e);
            }
        }

        return invoker.invoke(invocation)
                .onFinish((r, t) -> {
                    if (Objects.isNull(t)) {
                        //success
                        Set<ConstraintViolation<Object>> violations = validator.validate(r, groups);
                        if (!violations.isEmpty()) {
                            throw new ConstraintViolationException(
                                    toErrorString("fail to validate rpc call result, handler=%s, due to %s", handler, violations),
                                    violations);
                        }
                    }
                });
    }

    @Override
    public int order() {
        return HIGHEST_ORDER;
    }

    private static String toErrorString(String msg,
                                        String handler,
                                        Set<? extends ConstraintViolation<?>> constraintViolations) {
        String violationsStr = constraintViolations.stream()
                .map(cv -> cv == null ? "null" : cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        return String.format(msg, handler, violationsStr);
    }

    /**
     * 创建{@link MethodValidationMetadata}实例
     *
     * @param invocation rpc call info
     * @return {@link MethodValidationMetadata}实例
     */
    private MethodValidationMetadata createMethodValidationMetadata(Invocation invocation) {
        Class<?>[] groups = determineValidationGroups(invocation);
        Object[] params = invocation.params();
        Method method = invocation.method();
        List<Short> validateParamIdxList = new ArrayList<>(params.length);
        List<Parameter> validParameters = new ArrayList<>(params.length);
        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];

            //遍历寻找带@Constraint的注解
            Annotation constraintAnno = null;
            for (Annotation annotation : parameter.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                    constraintAnno = annotation;
                    break;
                }
            }

            if (Objects.isNull(constraintAnno)) {
                //没有带@Constraint的注解
                continue;
            }

            validateParamIdxList.add((short) i);
            validParameters.add(parameter);
        }

        short[] validateParamIdxes = new short[validateParamIdxList.size()];
        for (int i = 0; i < validateParamIdxList.size(); i++) {
            validateParamIdxes[i] = validateParamIdxList.get(i);
        }
        return new MethodValidationMetadata(groups, validateParamIdxes, generateParameterBeanConstructor(method, validParameters));
    }

    /**
     * 生成用于校验的parameter bean
     * 生成bean类, 包含服务方法中带{@link Valid}注解的参数字段, 校验前时, 创建该bean实例并将参数复制, 然后去校验该bean
     * 最后的效果就是相当于校验了服务方法中带{@link Valid}注解的参数字段
     * <p>
     * !!!需要类增强技术支持
     *
     * @param method          服务方法
     * @param validParameters 服务方法需要校验的参数
     * @return parameter bean constructor
     */
    @Nullable
    private Constructor<?> generateParameterBeanConstructor(Method method, List<Parameter> validParameters) {
        if (CollectionUtils.isEmpty(validParameters)) {
            return null;
        }

        if (!KinRpcAppContext.ENHANCE) {
            //没有类增强技术, 则不校验
            log.warn("can not valid primitives parameter, because generate parameter bean constructor fail due to can not find ByteBuddy support in classpath");
            return null;
        }

        StringJoiner builder = new StringJoiner("$");
        builder.add(method.getDeclaringClass().getName());
        builder.add(method.getName());
        builder.add("Parameter");
        int validParametersHashCode = 0;
        for (Parameter parameter : validParameters) {
            validParametersHashCode += parameter.getType().hashCode();
        }
        if (validParametersHashCode > 0) {
            builder.add(String.valueOf(validParametersHashCode));
        } else {
            //负数用Neg代替-
            builder.add("Neg" + Math.abs(validParametersHashCode));
        }

        DynamicType.Builder<Object> dynamicTypeBuilder = new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(Object.class)
                .name(builder.toString());

        Class<?>[] validParameterTypes = new Class<?>[validParameters.size()];
        try {
            Implementation.Composable constructorMethodCall = MethodCall.invoke(Object.class.getDeclaredConstructor());
            for (int i = 0; i < validParameters.size(); i++) {
                Parameter validParameter = validParameters.get(i);
                String fieldName = validParameter.getName();
                Class<?> type = validParameter.getType();
                validParameterTypes[i] = type;

                //定义constructor逻辑
                constructorMethodCall = constructorMethodCall.andThen(FieldAccessor.ofField(fieldName).setsArgumentAt(i));

                //定义field
                dynamicTypeBuilder = dynamicTypeBuilder.defineField(fieldName, type, Modifier.PRIVATE)
                        .annotateField(validParameter.getAnnotations())
                        //定义field setter
                        .defineMethod("set" + StringUtils.firstUpperCase(fieldName), Void.TYPE, Modifier.PUBLIC)
                        .withParameters(type)
                        .intercept(FieldAccessor.ofField(fieldName))
                        //定义field getter
                        .defineMethod("get" + StringUtils.firstUpperCase(fieldName), type, Modifier.PUBLIC)
                        .intercept(FieldAccessor.ofField(fieldName));
            }

            //定义constructor
            dynamicTypeBuilder = dynamicTypeBuilder.defineConstructor(Modifier.PUBLIC)
                    .withParameters(validParameterTypes)
                    .intercept(constructorMethodCall);

            Class<?> parameterBeanClass = dynamicTypeBuilder.make()
                    .load(method.getDeclaringClass().getClassLoader())
                    .getLoaded();

            return parameterBeanClass.getConstructor(validParameterTypes);
        } catch (Exception e) {
            throw new RpcException("generate parameter bean constructor fail", e);
        }
    }

    /**
     * 校验服务方法参数
     *
     * @param param  服务方法参数
     * @param groups validation groups
     */
    private void validate(String handler,
                          Parameter parameter,
                          Object param,
                          Class<?>[] groups) {
        if (Objects.isNull(param) ||
                isPrimitives(param.getClass())) {
            return;
        }

        if (param instanceof Object[]) {
            for (Object item : ((Object[]) param)) {
                validate(handler, parameter, item, groups);
            }
        } else if (param instanceof Collection) {
            for (Object item : ((Collection<?>) param)) {
                validate(handler, parameter, item, groups);
            }
        } else if (param instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) param).entrySet()) {
                validate(handler, parameter, entry.getKey(), groups);
                validate(handler, parameter, entry.getValue(), groups);
            }
        } else {
            Set<ConstraintViolation<Object>> violations = validator.validate(param, groups);
            if (CollectionUtils.isNonEmpty(violations)) {
                throw new ConstraintViolationException(
                        toErrorString("fail to validate rpc call params, handler=%s, due to param " + parameter.getName() + " %s", handler, violations),
                        violations);
            }
        }
    }

    /**
     * 返回{@code cls}是否是primitive, {@link Number}, {@link Date}
     * 如果{@code cls}是数组, 则取其component type来判断
     *
     * @param cls class
     */
    private static boolean isPrimitives(Class<?> cls) {
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }
        return isPrimitive(cls);
    }

    /**
     * 返回{@code cls}是否是primitive, {@link Number}, {@link Date}
     *
     * @param cls class
     */
    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == String.class || cls == Boolean.class || cls == Character.class
                || Number.class.isAssignableFrom(cls) || Date.class.isAssignableFrom(cls);
    }

    /**
     * Determine the validation groups to validate against for the given method invocation.
     *
     * @param invocation rpc call info
     * @return the applicable validation groups as a Class array
     */
    private Class<?>[] determineValidationGroups(Invocation invocation) {
        List<Class<?>> groups = new ArrayList<>();

        Method method = invocation.method();
        Class<?> declaringClass = method.getDeclaringClass();
        Validated validated = declaringClass.getAnnotation(Validated.class);
        if (Objects.nonNull(validated)) {
            groups.addAll(Arrays.asList(validated.value()));
        }

        validated = method.getAnnotation(Validated.class);
        if (Objects.nonNull(validated)) {
            groups.addAll(Arrays.asList(validated.value()));
        }

        groups.add(0, Default.class);
        groups.add(1, declaringClass);

        return groups.toArray(EMPTY_CLASS_ARR);
    }
}
