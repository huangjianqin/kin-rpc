package org.kin.kinrpc;

import com.google.common.base.Preconditions;
import javassist.*;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.*;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.transport.Protocol;

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
    public final <T> Exporter<T> export(Invoker<T> invoker) {
        Url url = invoker.url();
        Class<T> interfaceC = invoker.getInterface();

        boolean useByteCode = Boolean.parseBoolean(url.getParam(Constants.BYTE_CODE_INVOKE_KEY));
        T proxy;
        if (useByteCode) {
            proxy = javassistProxyedInvoker(invoker, interfaceC);
        } else {
            proxy = reflectProxyedInvoker(invoker, interfaceC);
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
            }
        };
    }

    @Override
    public final <T> AsyncInvoker<T> reference(Url url) {
        Class<T> interfaceC;
        try {
            interfaceC = (Class<T>) Class.forName(url.getInterfaceN());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        T proxy = doReference(interfaceC, url);
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
                        throw new RuntimeException(throwable);
                    }
                }, RpcThreadPool.EXECUTORS);
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public void destroy() {

            }
        };
    }

    /**
     * @param impl       实现了服务接口的代理invoker
     * @param interfaceC 服务接口
     * @param url        服务url
     * @param <T>        服务接口
     * @return destory逻辑方法
     */
    protected abstract <T> Runnable doExport(T impl, Class<T> interfaceC, Url url);

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
    private <T> T reflectProxyedInvoker(Invoker<T> invoker, Class<T> interfaceC) {
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
    private <T> T javassistProxyedInvoker(Invoker<T> invoker, Class<T> interfaceC) {
        Class<? extends AbstractProxyProtocol> myClass = getClass();
        String ctClassName =
                myClass.getPackage().getName().concat(".")
                        .concat(myClass.getSimpleName())
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
                        CtMethod ctMethod = CtMethod.make(ClassUtils.generateMethodContent(method, generateServiceMethodBody(method)), proxyClass);
                        proxyClass.addMethod(ctMethod);
                    }

                    //生成服务接口方法方法体
                    for (Method method : GenericRpcService.class.getDeclaredMethods()) {
                        CtMethod ctMethod = CtMethod.make(ClassUtils.generateMethodContent(method, generateGenericRpcServiceMethodBody()), proxyClass);
                        proxyClass.addMethod(ctMethod);
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

    private String generateServiceMethodBody(Method method) {
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
                String argStr = "arg".concat(Integer.toString(i));
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
        methodBody.append("throw new RuntimeException(e);").append(System.lineSeparator());
        methodBody.append("}").append(System.lineSeparator());

        return methodBody.toString();
    }

    /**
     * 生成限流代码
     */
    private String generateGenericRpcServiceMethodBody() {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //rpc call代码
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(".invoke"));
        invokeCode.append("(arg0, arg1);");
        String invokeCodeStr = invokeCode.toString();

        //return
        methodBody.append("return ").append(invokeCodeStr).append(System.lineSeparator());

        return methodBody.toString();
    }
}
