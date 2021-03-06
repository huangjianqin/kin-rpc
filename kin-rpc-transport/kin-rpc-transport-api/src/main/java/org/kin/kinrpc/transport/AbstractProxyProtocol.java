package org.kin.kinrpc.transport;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.proxy.JavassistFactory;
import org.kin.framework.proxy.Javassists;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Exporter;
import org.kin.kinrpc.rpc.GenericRpcService;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.invoker.ProxyedInvoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.StringJoiner;

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
            proxy = javassistProxyProviderInvoker(invoker, interfaceC);
        } else {
            proxy = jdkProxyProviderInvoker(invoker, interfaceC);
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
                Javassists.detach(proxy.getClass().getName());
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
        return generateAsyncInvoker(url, interfaceC, doReference(interfaceC, url), url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY), null);
    }

    /**
     * 为代理协议生成Invoker
     */
    protected <T> AsyncInvoker<T> generateAsyncInvoker(Url url, Class<T> interfaceC, T proxy, boolean byteCodeInvoke, Runnable destroyMethod) {
        return ProxyedInvoker.proxyedProviderInvoker(url, proxy, interfaceC, byteCodeInvoke, () -> {
            //释放无用代理类
            Javassists.detach(proxy.getClass().getName());

            //自定义销毁操作
            if (Objects.nonNull(destroyMethod)) {
                destroyMethod.run();
            }
        });
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
    protected <T> T jdkProxyProviderInvoker(Invoker<T> invoker, Class<T> interfaceC) {
        return (T) Proxy.newProxyInstance(interfaceC.getClassLoader(), new Class<?>[]{interfaceC, GenericRpcService.class}, (proxy, method, args) -> {
            if (GenericRpcService.class.equals(method.getDeclaringClass())) {
                return invoker.invoke(args[0].toString(), (Object[]) args[1]);
            }
            return invoker.invoke(method.getName(), args);
        });
    }

    /**
     * 代理provider invoker的invoker, 相当于reference了, 但其实是provider, provider端的reference
     * 基于javassist代理的invoker
     */
    @SuppressWarnings("unchecked")
    protected <T> T javassistProxyProviderInvoker(Invoker<T> invoker, Class<T> interfaceC) {
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
            //ignore
        }

        try {
            if (Objects.isNull(realProxyClass)) {
                ClassPool classPool = Javassists.getPool();
                CtClass proxyClass = classPool.getOrNull(ctClassName);

                if (Objects.isNull(proxyClass)) {
                    proxyClass = classPool.makeClass(ctClassName);
                    //服务接口
                    proxyClass.addInterface(classPool.get(interfaceC.getName()));
                    //通用接口
                    proxyClass.addInterface(classPool.get(GenericRpcService.class.getName()));

                    CtField invokerField = new CtField(classPool.get(Invoker.class.getName()), JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(Invoker.class.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME).concat(" = $1;").concat("}"));
                    proxyClass.addConstructor(constructor);

                    //生成服务接口方法方法体
                    for (Method method : interfaceC.getDeclaredMethods()) {
                        Javassists.makeCtPublicFinalMethod(classPool, method, generateProviderServiceMethodBody(method), proxyClass);
                    }

                    //生成通用服务接口方法方法体
                    for (Method method : GenericRpcService.class.getDeclaredMethods()) {
                        Javassists.makeCtPublicFinalMethod(classPool, method, generateProviderGenericRpcServiceMethodBody(), proxyClass);
                    }

                    Javassists.cacheCTClass(ctClassName, proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(Invoker.class).newInstance(invoker);
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
        return null;
    }

    private String generateProviderServiceMethodBody(Method method) {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //rpc call代码
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME.concat(".invoke"));
        invokeCode.append("(\"").append(method.getName()).append("\"");

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            invokeCode.append(", new Object[]{");
            StringJoiner invokeBody = new StringJoiner(", ");
            for (int i = 0; i < parameterTypes.length; i++) {
                String argStr = JavassistFactory.METHOD_DECLARATION_PARAM_NAME.concat(Integer.toString(i + 1));
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
     * 生成generic provider代码
     */
    private String generateProviderGenericRpcServiceMethodBody() {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //rpc call代码
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME.concat(".invoke"));
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
    protected <T> T jdkProxyGenericRpcService(GenericRpcService genericRpcService, Class<T> interfaceC) {
        return (T) Proxy.newProxyInstance(interfaceC.getClassLoader(), new Class<?>[]{interfaceC}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return genericRpcService.invoke(method.getName(), args);
            }
        });
    }

    /**
     * 代理类为{@link GenericRpcService}的服务接口实现类
     * 基于javassist
     */
    @SuppressWarnings("unchecked")
    protected <T> T javassistProxyGenericRpcService(GenericRpcService genericRpcService, Class<T> interfaceC) {
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
            //ignore
        }

        try {
            if (Objects.isNull(realProxyClass)) {
                ClassPool classPool = Javassists.getPool();
                CtClass proxyClass = classPool.getOrNull(ctClassName);

                if (Objects.isNull(proxyClass)) {
                    proxyClass = classPool.makeClass(ctClassName);
                    //服务接口
                    proxyClass.addInterface(classPool.get(interfaceC.getName()));
                    //通用接口
                    proxyClass.addInterface(classPool.get(GenericRpcService.class.getName()));

                    CtField invokerField = new CtField(classPool.get(GenericRpcService.class.getName()), JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(GenericRpcService.class.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME).concat(" = $1;").concat("}"));
                    proxyClass.addConstructor(constructor);

                    //生成服务接口方法方法体
                    for (Method method : interfaceC.getDeclaredMethods()) {
                        Javassists.makeCtPublicFinalMethod(classPool, method, generateProviderServiceMethodBody(method), proxyClass);

                    }

                    //生成通用服务接口方法方法体
                    for (Method method : GenericRpcService.class.getDeclaredMethods()) {
                        Javassists.makeCtPublicFinalMethod(classPool, method, generateProviderGenericRpcServiceMethodBody(), proxyClass);
                    }

                    Javassists.cacheCTClass(ctClassName, proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(GenericRpcService.class).newInstance(genericRpcService);
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
        return null;
    }
}
