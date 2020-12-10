package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import javassist.*;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2019-09-09
 */
class JavassistClusterInvoker<T> extends ClusterInvoker<T> {
    private Class<T> interfaceClass;
    private int rate;

    public JavassistClusterInvoker(Cluster<T> cluster, Url url, Class<T> interfaceClass, List<Notifier<?>> notifiers) {
        super(cluster, url, notifiers);
        this.interfaceClass = interfaceClass;
        this.rate = url.getIntParam(Constants.RATE_KEY);
    }

    public static <T> T proxy(Cluster<T> cluster, Url url, Class<T> interfaceClass, List<Notifier<?>> notifiers) {
        return new JavassistClusterInvoker<>(cluster, url, interfaceClass, notifiers).proxy();
    }

    //------------------此处需自定义, 有点特殊-------------------------------------

    private String generateMethodBody(Method method) {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //rpc call代码
        String futureVarName = "future";
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(CompletableFuture.class.getName()).append(" ").append(futureVarName).append(" = ");
        invokeCode.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(".invokeAsync"));
        invokeCode.append("(\"").append(ClassUtils.getUniqueName(method)).append("\"");
        invokeCode.append(", ").append(method.getReturnType().getName()).append(".class");

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
        methodBody.append(invokeCode.toString()).append(";").append(System.lineSeparator());

        //after rpc call逻辑
        Class<?> returnType = method.getReturnType();
        boolean isVoid = Void.class.equals(returnType) || Void.TYPE.equals(returnType);

        methodBody.append("if(").append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).append(".isAsync()){").append(System.lineSeparator());
        methodBody.append(RpcContext.class.getName()).append(".updateFuture(".concat(futureVarName).concat(");")).append(System.lineSeparator());
        //方法返回
        if (!isVoid) {
            if (returnType.isPrimitive()) {
                //基础类型
                methodBody.append("return ").append(ClassUtils.getDefaultValue(returnType)).append(";").append(System.lineSeparator());
            } else {
                methodBody.append("return null;").append(System.lineSeparator());
            }
        }
        methodBody.append("}").append(System.lineSeparator());
        methodBody.append("else{").append(System.lineSeparator());
        methodBody.append("try{").append(System.lineSeparator());
        //方法返回
        if (isVoid) {
            methodBody.append(futureVarName.concat(".get()"))
                    .append(";").append(System.lineSeparator());
        } else {
            if (returnType.isPrimitive()) {
                //基础类型
                methodBody.append("return ")
                        .append(ClassUtils.primitiveUnpackage(returnType, futureVarName.concat(".get()")))
                        .append(";").append(System.lineSeparator());
            } else {
                methodBody.append("return ")
                        .append("(").append(returnType.getName()).append(")").append(futureVarName.concat(".get()"))
                        .append(";").append(System.lineSeparator());
            }

        }

        methodBody.append("} catch (Exception e) {").append(System.lineSeparator());
        methodBody.append("throw new ").append(RpcCallErrorException.class.getName()).append("(e);").append(System.lineSeparator());
        methodBody.append("}").append(System.lineSeparator());
        methodBody.append("}");

        return methodBody.toString();
    }

    /**
     * 生成限流代码
     */
    private String generateRateLimitBody(String rateLimiterFieldName, Method method) throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append("if(").append(Objects.class.getName()).append(".nonNull(").append(rateLimiterFieldName).append(") ")
                .append("&& !").append(rateLimiterFieldName).append(".tryAcquire()){").append(System.lineSeparator())
                .append("throw new ").append(RateLimitException.class.getName()).append("(\"")
                .append(interfaceClass.getName()).append("$").append(method.toString()).append("\");").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    /**
     * 构建javassist字节码增强代理类
     */
    public T proxy() {
        Class<? extends JavassistClusterInvoker> myClass = this.getClass();
        String ctClassName =
                myClass.getPackage().getName().concat(".")
                        .concat(myClass.getSimpleName())
                        .concat("$")
                        .concat(interfaceClass.getSimpleName());
        Class<T> realProxyClass = null;
        try {
            realProxyClass = (Class<T>) Class.forName(ctClassName);
        } catch (ClassNotFoundException e) {
            ExceptionUtils.throwExt(e);
        }

        try {
            if (Objects.isNull(realProxyClass)) {
                ClassPool classPool = ProxyEnhanceUtils.getPool();
                CtClass proxyClass = classPool.getOrNull(ctClassName);

                if (Objects.isNull(proxyClass)) {
                    proxyClass = classPool.makeClass(ctClassName);
                    //接口
                    proxyClass.addInterface(classPool.get(interfaceClass.getName()));

                    CtField invokerField = new CtField(classPool.get(myClass.getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    String rateLimiterFieldName = "rateLimiter";
                    CtField rateLimiterField = new CtField(classPool.get(RateLimiter.class.getName()), rateLimiterFieldName, proxyClass);
                    rateLimiterField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(rateLimiterField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(myClass.getName()), classPool.get(Double.TYPE.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat(" = $1;")
                            .concat("if($2 > 0){$0.".concat(rateLimiterFieldName).concat(" = ").concat(RateLimiter.class.getName()).concat(".create($2);}}")));
                    proxyClass.addConstructor(constructor);

                    //生成接口方法方法体
                    for (Method method : interfaceClass.getDeclaredMethods()) {
                        StringBuilder methodBody = new StringBuilder();
                        methodBody.append(generateRateLimitBody(rateLimiterFieldName, method)).append(System.lineSeparator());
                        methodBody.append(generateMethodBody(method)).append(System.lineSeparator());

                        CtMethod ctMethod = CtMethod.make(ClassUtils.generateMethodContent(method, methodBody.toString()), proxyClass);
                        proxyClass.addMethod(ctMethod);
                    }
                    ProxyEnhanceUtils.cacheCTClass(ctClassName, proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(myClass, Double.TYPE).newInstance(this, (double) rate);
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
        return null;
    }

    /**
     * 获取代理类名
     */
    private String proxyClassName() {
        return this.getClass().getPackage().getName().concat(".")
                .concat(this.getClass().getSimpleName())
                .concat("$")
                .concat(interfaceClass.getSimpleName());
    }

    //----------------------------------------------------------------------------------------------------

    @Override
    public void close() {
        super.close();
        ProxyEnhanceUtils.detach(proxyClassName());
    }
}
