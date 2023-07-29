package org.kin.kinrpc.validation;

import org.kin.kinrpc.RpcException;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/27
 */
public class MethodValidationMetadata {
    /** validation groups */
    private final Class<?>[] groups;
    /** 要校验的参数数组下标 */
    private final short[] validateParamIdxes;
    /** parameter bean constructor */
    private final Constructor<?> parameterBeanConstructor;

    public MethodValidationMetadata(Class<?>[] groups, short[] validateParamIdxes, Constructor<?> parameterBeanConstructor) {
        this.groups = groups;
        this.validateParamIdxes = validateParamIdxes;
        this.parameterBeanConstructor = parameterBeanConstructor;
    }

    /**
     * 创建parameter bean
     *
     * @param params 服务方法调用参数
     * @return parameter bean instance
     */
    @Nullable
    public Object createParameterBean(Object[] params) {
        Object[] validParams = new Object[validateParamIdxes.length];
        for (int i = 0; i < validateParamIdxes.length; i++) {
            validParams[i] = params[validateParamIdxes[i]];
        }

        if (Objects.isNull(parameterBeanConstructor)) {
            return null;
        }

        try {
            return parameterBeanConstructor.newInstance(validParams);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RpcException("instantiate parameter bean fail", e);
        }
    }

    //getter
    public Class<?>[] getGroups() {
        return groups;
    }

    public short[] getValidateParamIdxes() {
        return validateParamIdxes;
    }

    public Constructor<?> getParameterBeanConstructor() {
        return parameterBeanConstructor;
    }
}
