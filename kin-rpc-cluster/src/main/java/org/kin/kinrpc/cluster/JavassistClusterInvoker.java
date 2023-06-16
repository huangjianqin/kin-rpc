package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import org.kin.framework.proxy.JavassistFactory;
import org.kin.framework.proxy.Javassists;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.TpsLimitException;

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
    private final Class<T> interfaceClass;
    private final int tps;

    public JavassistClusterInvoker(Cluster<T> cluster, Url url, Class<T> interfaceClass, List<Notifier<?>> notifiers) {
        super(cluster, url, notifiers);
        this.interfaceClass = interfaceClass;
        this.tps = url.getIntParam(Constants.TPS_KEY);
    }

    public static <T> T proxy(Cluster<T> cluster, Url url, Class<T> interfaceClass, List<Notifier<?>> notifiers) {
        return new JavassistClusterInvoker<>(cluster, url, interfaceClass, notifiers).proxy();
    }

    //------------------此处需自定义, 有点特殊-------------------------------------

    /**
     * 本质上参考{@link JdkProxyClusterInvoker invoke}方法逻辑写的
     */
    private String generateMethodBody(Method method) {
        //真正逻辑
        StringBuilder methodBody = new StringBuilder();

        //返回值类型定义
        Class<?> returnType = method.getReturnType();
        String returnTypeVarName = "returnType";
        methodBody.append("Class ")
                .append(returnTypeVarName)
                .append(" = ")
                .append(returnType.getName())
                .append(".class;")
                .append(System.lineSeparator());

        //rpc call代码
        String futureVarName = "future";
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(CompletableFuture.class.getName()).append(" ").append(futureVarName).append(" = ");
        invokeCode.append(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME.concat(".invokeAsync"));
        invokeCode.append("(\"").append(method.getName()).append("\"");
        invokeCode.append(", ").append("returnType");

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
        methodBody.append(invokeCode.toString()).append(";").append(System.lineSeparator());

        //after rpc call逻辑
        boolean isVoid = Void.class.equals(returnType) || Void.TYPE.equals(returnType);

        //返回值处理
        StringBuilder handleResultCode = new StringBuilder();
        handleResultCode.append(RpcCallReturnAdapters.class.getName())
                .append(".INSTANCE.handleReturn(")
                .append(returnTypeVarName).append(", ")
                //获取是否rpc async call
                .append(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME).append(".isAsync(), ")
                .append(futureVarName)
                .append(")");

        if (!isVoid) {
            //返回值非空
            methodBody.append("return ");
            if (returnType.isPrimitive()) {
                //基础类型
                methodBody.append(ClassUtils.primitiveUnpackage(returnType, handleResultCode.toString()))
                        .append(";").append(System.lineSeparator());
            } else {
                //强转类型
                methodBody.append("(")
                        .append(returnType.getName())
                        .append(")")
                        .append(handleResultCode.toString())
                        .append(";")
                        .append(System.lineSeparator());
            }
        } else {
            methodBody.append(handleResultCode.toString())
                    .append(";")
                    .append(System.lineSeparator());
        }

        return methodBody.toString();
    }

    /**
     * 生成限流代码
     */
    private String generateRateLimitBody(String rateLimiterFieldName, Method method) throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append("if(").append(Objects.class.getName()).append(".nonNull(").append(rateLimiterFieldName).append(") ")
                .append("&& !").append(rateLimiterFieldName).append(".tryAcquire()){").append(System.lineSeparator())
                .append("throw new ").append(TpsLimitException.class.getName()).append("(\"")
                .append(interfaceClass.getName()).append("$").append(method.toString()).append("\");").append(System.lineSeparator())
                .append("}");
        return sb.toString();
    }

    /**
     * 构建javassist字节码增强代理类
     */
    @SuppressWarnings("unchecked")
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
            //ignore
        }

        try {
            if (Objects.isNull(realProxyClass)) {
                ClassPool classPool = Javassists.getPool();
                CtClass proxyClass = classPool.getOrNull(ctClassName);

                if (Objects.isNull(proxyClass)) {
                    proxyClass = classPool.makeClass(ctClassName);
                    //接口
                    proxyClass.addInterface(classPool.get(interfaceClass.getName()));

                    CtField invokerField = new CtField(classPool.get(myClass.getName()), JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    String tpsLimiterFieldName = "tpsLimiter";
                    CtField rateLimiterField = new CtField(classPool.get(RateLimiter.class.getName()), tpsLimiterFieldName, proxyClass);
                    rateLimiterField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(rateLimiterField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(myClass.getName()), classPool.get(Double.TYPE.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(JavassistFactory.DEFAULT_INSTANCE_FIELD_NAME).concat(" = $1;")
                            .concat("if($2 > 0){$0.".concat(tpsLimiterFieldName).concat(" = ").concat(RateLimiter.class.getName()).concat(".create($2);}}")));
                    proxyClass.addConstructor(constructor);

                    //加载RpcCallReturnAdapters类
                    classPool.get(RpcCallReturnAdapters.class.getName());
                    //生成接口方法方法体
                    for (Method method : interfaceClass.getDeclaredMethods()) {
                        StringBuilder methodBody = new StringBuilder();
                        methodBody.append(generateRateLimitBody(tpsLimiterFieldName, method)).append(System.lineSeparator());
                        methodBody.append(generateMethodBody(method)).append(System.lineSeparator());

                        Javassists.makeCtPublicFinalMethod(classPool, method, methodBody.toString(), proxyClass);
                    }
                    Javassists.cacheCTClass(ctClassName, proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(myClass, Double.TYPE).newInstance(this, (double) tps);
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
        Javassists.detach(proxyClassName());
    }
}
