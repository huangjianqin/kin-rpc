package org.kin.kinrpc.transport;

import com.google.common.base.Preconditions;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.*;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/**
 * 需代理的传输协议
 *
 * @author huangjianqin
 * @date 2020/11/17
 */
public abstract class AbstractProxyProtocol implements Protocol, LoggerOprs {
    @Override
    public <T> Exporter<T> export(ProviderInvoker<T> invoker) {
        Url url = invoker.url();
        Class<T> interfaceC = invoker.getInterface();

        boolean useByteCode = url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY);
        T proxy;
        if (useByteCode) {
            proxy = javassistProxyedProviderInvoker(invoker, interfaceC);
        } else {
            proxy = reflectProxyedProviderInvoker(invoker, interfaceC);
        }

        Runnable destroyRunnable = doExport(proxy, interfaceC, url);
        return new Exporter<T>() {
            @Override
            public Invoker<T> getInvoker() {
                return invoker;
            }

            @Override
            public void unexport() {
                invoker.destroy();
                destroyRunnable.run();
                //释放无用代理类
                ProxyEnhanceUtils.detach(proxy.getClass().getName());
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> AsyncInvoker<T> reference(Url url) throws Throwable {
        Class<T> interfaceC;
        try {
            interfaceC = (Class<T>) Class.forName(url.getInterfaceN());
        } catch (ClassNotFoundException e) {
            throw e;
        }
        return generateAsyncInvoker(url, interfaceC, doReference(interfaceC, url), null);
    }

    /**
     * 为代理协议生成Invoker
     */
    protected <T> AsyncInvoker<T> generateAsyncInvoker(Url url, Class<T> interfaceC, T proxy, Runnable destroyMethod) {
        return new ReferenceInvoker<T>(url) {

            @Override
            public Object invoke(String methodName, Object... params) throws Throwable {
                Method targetMethod = null;
                for (Method method : interfaceC.getMethods()) {
                    //用方法名生成某一标识判断
                    if (methodName.equals(ClassUtils.getUniqueName(method))) {
                        targetMethod = method;
                        break;
                    }
                }

                Preconditions.checkNotNull(targetMethod, String.format("can't not find method '%s'", methodName));

                return targetMethod.invoke(proxy, params);
            }

            @Override
            public CompletableFuture<Object> invokeAsync(String methodName, Object... params) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return invoke(methodName, params);
                    } catch (Throwable throwable) {
                        throw new IllegalStateException(throwable);
                    }
                }, RpcThreadPool.executors());
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public void destroy() {
                //释放无用代理类
                ProxyEnhanceUtils.detach(proxy.getClass().getName());

                //自定义销毁操作
                if (Objects.nonNull(destroyMethod)) {
                    destroyMethod.run();
                }
            }
        };
    }

    /**
     * @param proxyedInvoker 实现了服务接口的代理invoker
     * @param interfaceC     服务接口
     * @param url            服务url
     * @param <T>            服务接口
     * @return destory逻辑方法
     */
    protected abstract <T> Runnable doExport(T proxyedInvoker, Class<T> interfaceC, Url url);

    /**
     * @param interfaceC 服务接口
     * @param url        服务url
     * @param <T>        服务接口
     * @return 代理实现类
     */
    protected abstract <T> T doReference(Class<T> interfaceC, Url url);
    //----------------------------------------------------------------------------------------------------------------

    /**
     * 代理invoker的invoker, 相当于reference了, 但其实是provider, provider端的reference
     * 基于反射代理的invoker
     */
    @SuppressWarnings("unchecked")
    protected <T> T reflectProxyedProviderInvoker(Invoker<T> invoker, Class<T> interfaceC) {
        return (T) Proxy.newProxyInstance(interfaceC.getClassLoader(), new Class<?>[]{interfaceC, GenericRpcService.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (GenericRpcService.class.equals(method.getDeclaringClass())) {
                    return invoker.invoke(args[0].toString(), (Object[]) args[1]);
                }
                return invoker.invoke(ClassUtils.getUniqueName(method), args);
            }
        });
    }

    /**
     * 代理invoker的invoker, 相当于reference了, 但其实是provider, provider端的reference
     * 基于javassist代理的invoker
     */
    @SuppressWarnings("unchecked")
    protected <T> T javassistProxyedProviderInvoker(Invoker<T> invoker, Class<T> interfaceC) {
        Class<? extends AbstractProxyProtocol> myClass = getClass();
        String ctClassName =
                myClass.getPackage().getName().concat(".")
                        .concat(myClass.getSimpleName())
                        .concat("$")
                        .concat("Provider")
                        .concat("$")
                        .concat(interfaceC.getSimpleName());
        Class<T> realProxyClass = null;
        try {
            realProxyClass = (Class<T>) Class.forName(ctClassName);
        } catch (ClassNotFoundException e) {

        }

        try {
            if (Objects.isNull(realProxyClass)) {
                ClassPool classPool = ProxyEnhanceUtils.getPool();
                CtClass proxyClass = classPool.getOrNull(ctClassName);

                if (Objects.isNull(proxyClass)) {
                    proxyClass = classPool.makeClass(ctClassName);
                    //服务接口
                    proxyClass.addInterface(classPool.get(interfaceC.getName()));
                    //通用接口
                    proxyClass.addInterface(classPool.get(GenericRpcService.class.getName()));

                    CtField invokerField = new CtField(classPool.get(Invoker.class.getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(Invoker.class.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat(" = $1;").concat("}"));
                    proxyClass.addConstructor(constructor);

                    //生成服务接口方法方法体
                    for (Method method : interfaceC.getDeclaredMethods()) {
                        ProxyEnhanceUtils.makeCtPublicFinalMethod(classPool, method, generateProviderServiceMethodBody(method), proxyClass);
                    }

                    //生成通用服务接口方法方法体
                    for (Method method : GenericRpcService.class.getDeclaredMethods()) {
                        ProxyEnhanceUtils.makeCtPublicFinalMethod(classPool, method, generateProviderGenericRpcServiceMethodBody(), proxyClass);
                    }

                    ProxyEnhanceUtils.cacheCTClass(ctClassName, proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(Invoker.class).newInstance(invoker);
        } catch (Exception e) {
            error(e.getMessage(), e);
        }
        return null;
    }

    private String generateProviderServiceMethodBody(Method method) {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //rpc call代码
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(".invoke"));
        invokeCode.append("(\"").append(ClassUtils.getUniqueName(method)).append("\"");

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            invokeCode.append(", new Object[]{");
            StringJoiner invokeBody = new StringJoiner(", ");
            for (int i = 0; i < parameterTypes.length; i++) {
                String argStr = ProxyEnhanceUtils.METHOD_DECLARATION_PARAM_NAME.concat(Integer.toString(i + 1));
                invokeBody.add(org.kin.framework.utils.ClassUtils.primitivePackage(parameterTypes[i], argStr));
            }
            invokeCode.append(invokeBody.toString());
            invokeCode.append("})");
        } else {
            invokeCode.append(", new Object[0])");
        }
        String invokeCodeStr = invokeCode.toString();

        //after rpc call逻辑
        Class<?> returnType = method.getReturnType();
        boolean isVoid = Void.class.equals(returnType) || Void.TYPE.equals(returnType);

        methodBody.append("try{").append(System.lineSeparator());
        //方法返回
        if (isVoid) {
            methodBody.append(invokeCodeStr).append(";").append(System.lineSeparator());
        } else {
            if (returnType.isPrimitive()) {
                //基础类型
                methodBody.append("return ")
                        .append(ClassUtils.primitiveUnpackage(returnType, invokeCodeStr))
                        .append(";").append(System.lineSeparator());
            } else {
                methodBody.append("return ")
                        .append("(").append(returnType.getName()).append(")").append(invokeCodeStr)
                        .append(";").append(System.lineSeparator());
            }

        }

        methodBody.append("} catch (Exception e) {").append(System.lineSeparator());
        methodBody.append("throw new ").append(RpcCallErrorException.class.getName()).append("(e);").append(System.lineSeparator());
        methodBody.append("}").append(System.lineSeparator());

        return methodBody.toString();
    }

    /**
     * 生成限流代码
     */
    private String generateProviderGenericRpcServiceMethodBody() {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //rpc call代码
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(".invoke"));
        invokeCode.append("($1, $2);");
        String invokeCodeStr = invokeCode.toString();

        //return
        methodBody.append("return ").append(invokeCodeStr).append(System.lineSeparator());

        return methodBody.toString();
    }

    /**
     * 代理类为{@link GenericRpcService}的服务接口实现类
     * 基于反射
     */
    @SuppressWarnings("unchecked")
    protected <T> T reflectProxyedGenericRpcService(GenericRpcService genericRpcService, Class<T> interfaceC) {
        return (T) Proxy.newProxyInstance(interfaceC.getClassLoader(), new Class<?>[]{interfaceC}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return genericRpcService.invoke(ClassUtils.getUniqueName(method), args);
            }
        });
    }

    /**
     * 代理类为{@link GenericRpcService}的服务接口实现类
     * 基于javassist
     */
    @SuppressWarnings("unchecked")
    protected <T> T javassistProxyedGenericRpcService(GenericRpcService genericRpcService, Class<T> interfaceC) {
        Class<? extends AbstractProxyProtocol> myClass = getClass();
        String ctClassName =
                myClass.getPackage().getName().concat(".")
                        .concat(myClass.getSimpleName())
                        .concat("$")
                        .concat(GenericRpcService.class.getSimpleName())
                        .concat("$")
                        .concat(interfaceC.getSimpleName());
        Class<T> realProxyClass = null;
        try {
            realProxyClass = (Class<T>) Class.forName(ctClassName);
        } catch (ClassNotFoundException e) {

        }

        try {
            if (Objects.isNull(realProxyClass)) {
                ClassPool classPool = ProxyEnhanceUtils.getPool();
                CtClass proxyClass = classPool.getOrNull(ctClassName);

                if (Objects.isNull(proxyClass)) {
                    proxyClass = classPool.makeClass(ctClassName);
                    //服务接口
                    proxyClass.addInterface(classPool.get(interfaceC.getName()));
                    //通用接口
                    proxyClass.addInterface(classPool.get(GenericRpcService.class.getName()));

                    CtField invokerField = new CtField(classPool.get(GenericRpcService.class.getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(GenericRpcService.class.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat(" = $1;").concat("}"));
                    proxyClass.addConstructor(constructor);

                    //生成服务接口方法方法体
                    for (Method method : interfaceC.getDeclaredMethods()) {
                        ProxyEnhanceUtils.makeCtPublicFinalMethod(classPool, method, generateProviderServiceMethodBody(method), proxyClass);

                    }

                    //生成通用服务接口方法方法体
                    for (Method method : GenericRpcService.class.getDeclaredMethods()) {
                        ProxyEnhanceUtils.makeCtPublicFinalMethod(classPool, method, generateProviderGenericRpcServiceMethodBody(), proxyClass);
                    }

                    ProxyEnhanceUtils.cacheCTClass(ctClassName, proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(GenericRpcService.class).newInstance(genericRpcService);
        } catch (Exception e) {
            error(e.getMessage(), e);
        }
        return null;
    }
}
