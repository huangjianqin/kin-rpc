package org.kin.kinrpc.cluster;

import com.google.common.util.concurrent.RateLimiter;
import javassist.*;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * @author huangjianqin
 * @date 2019-09-09
 */
class JavassistClusterInvoker<T> extends ClusterInvoker<T> {
    private Class<T> interfaceClass;
    private int rate;

    public JavassistClusterInvoker(Cluster<T> cluster, int retryTimes, long retryTimeout, Url url, Class<T> interfaceClass) {
        super(cluster, retryTimes, retryTimeout, url);
        this.interfaceClass = interfaceClass;
        this.rate = Integer.parseInt(url.getParam(Constants.RATE_KEY));
    }

    public static <T> T proxy(Cluster<T> cluster, int retryTimes, int retryTimeout, Url url, Class<T> interfaceClass) {
        return new JavassistClusterInvoker<>(cluster, retryTimes, retryTimeout, url, interfaceClass).proxy();
    }

    //------------------此处需自定义, 有点特殊-------------------------------------

    private String generateMethodBody(ClassPool classPool,
                                      CtClass proxyClass,
                                      Method method,
                                      Class[] parameterTypes,
                                      IntCounter internalClassNum) throws Exception {
        //真正逻辑
        StringBuffer methodBody = new StringBuffer();
        Class returnType = method.getReturnType();
        boolean isVoid = Void.class.equals(returnType) || Void.TYPE.equals(returnType);
        if (!isVoid) {
            methodBody.append("return ");
        }

        StringBuilder invokeCode = new StringBuilder();
        if (Future.class.equals(returnType)) {
            invokeCode.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(".invokeAsync"));
        } else {
            invokeCode.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(".invoke0"));
        }
        invokeCode.append("(\"".concat(ClassUtils.getUniqueName(method)).concat("\", ").concat(Boolean.toString(isVoid)));

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

        methodBody.append(org.kin.framework.utils.ClassUtils.primitiveUnpackage(returnType, invokeCode.toString()));
        methodBody.append(";");

        if (isVoid) {
            methodBody.append("return null;");
        }

        if (CompletableFuture.class.equals(returnType)) {
            //需要构造内部抽象类
            CtClass internalClass = classPool.makeClass(proxyClass.getName().concat("$").concat(Integer.toString(internalClassNum.getCount())));
            internalClass.addInterface(classPool.get(Supplier.class.getName()));

            Collection<CtClass> constructArgClass = new ArrayList<>();

            CtField internalClassField = new CtField(classPool.get(this.getClass().getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, internalClass);
            internalClassField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            internalClass.addField(internalClassField);
            constructArgClass.add(classPool.get(this.getClass().getName()));

            for (int i = 0; i < parameterTypes.length; i++) {
                CtField internalClassField1 = new CtField(classPool.get(parameterTypes[i].getName()), "arg".concat(Integer.toString(i)), internalClass);
                internalClassField1.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                internalClass.addField(internalClassField1);
                constructArgClass.add(classPool.get(parameterTypes[i].getName()));
            }

            //构造器
            CtConstructor internalClassConstructor = new CtConstructor(
                    constructArgClass.toArray(new CtClass[0]), internalClass);
            StringBuffer constructBody = new StringBuffer();
            constructBody.append("{");
            constructBody.append("$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat("= $1;"));
            for (int i = 1; i <= parameterTypes.length; i++) {
                constructBody.append("$0.arg".concat(Integer.toString((i - 1))).concat(" = $").concat(Integer.toString((i + 1))).concat(";"));
            }
            constructBody.append("}");
            internalClassConstructor.setBody(constructBody.toString());
            internalClass.addConstructor(internalClassConstructor);

            String internalMethodBody = "public Object get(){" +
                    methodBody.toString() +
                    "}";
            CtMethod internalMethod = CtMethod.make(internalMethodBody, internalClass);
            internalClass.addMethod(internalMethod);

            Class realInternalClass = internalClass.toClass();

            StringJoiner invokeParam = new StringJoiner(", ");
            invokeParam.add(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME);
            for (int i = 0; i < parameterTypes.length; i++) {
                invokeParam.add("arg".concat(Integer.toString(i)));
            }

            methodBody = new StringBuffer();
            methodBody.append("return ".concat(CompletableFuture.class.getName())
                    .concat(".supplyAsync(new ").concat(realInternalClass.getName()).concat("(").concat(invokeParam.toString()).concat("));"));

            internalClassNum.increment();
        }

        return methodBody.toString();
    }

    /**
     * 生成限流代码
     */
    private String generateRateLimitBody(String rateLimiterFieldName, Method method) throws Exception {
        StringBuffer sb = new StringBuffer();

        sb.append("if(").append(Objects.class.getName()).append(".nonNull(").append(rateLimiterFieldName).append(") ")
                .append("&& !").append(rateLimiterFieldName).append(".tryAcquire()){")
                .append("throw new ").append(RateLimitException.class.getName()).append("(\"")
                .append(interfaceClass.getName()).append("$").append(method.toString()).append("\");")
                .append("}");
        return sb.toString();
    }

    public T proxy() {
        String ctClassName = "org.kin.kinrpc.cluster.".concat(interfaceClass.getSimpleName()).concat("$JavassistProxy");
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
                    //接口
                    proxyClass.addInterface(classPool.get(interfaceClass.getName()));

                    CtField invokerField = new CtField(classPool.get(this.getClass().getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, proxyClass);
                    invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(invokerField);

                    String rateLimiterFieldName = "rateLimiter";
                    CtField rateLimiterField = new CtField(classPool.get(RateLimiter.class.getName()), rateLimiterFieldName, proxyClass);
                    rateLimiterField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    proxyClass.addField(rateLimiterField);

                    //构造器
                    CtConstructor constructor = new CtConstructor(
                            new CtClass[]{classPool.get(this.getClass().getName()), classPool.get(Double.TYPE.getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat(" = $1;")
                            .concat("if($2 > 0){$0.".concat(rateLimiterFieldName).concat(" = ").concat(RateLimiter.class.getName()).concat(".create($2);}}")));
                    proxyClass.addConstructor(constructor);

                    IntCounter internalClassNum = new IntCounter();
                    //生成接口方法方法体
                    for (Method method : interfaceClass.getDeclaredMethods()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("public ");
                        sb.append(method.getReturnType().getName().concat(" "));
                        sb.append(method.getName().concat("("));

                        StringJoiner paramBody = new StringJoiner(", ");

                        Class[] parameterTypes = method.getParameterTypes();
                        for (int i = 0; i < parameterTypes.length; i++) {
                            paramBody.add(parameterTypes[i].getName().concat(" ").concat("arg").concat(Integer.toString(i)));
                        }
                        sb.append(paramBody.toString().concat("){"));
                        sb.append(generateRateLimitBody(rateLimiterFieldName, method));
                        sb.append(generateMethodBody(classPool, proxyClass, method, parameterTypes, internalClassNum));
                        sb.append("}");

                        CtMethod ctMethod = CtMethod.make(sb.toString(), proxyClass);
                        proxyClass.addMethod(ctMethod);
                    }
                    ProxyEnhanceUtils.cacheCTClass(getUrl().getServiceName(), proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            return realProxyClass.getConstructor(this.getClass(), Double.TYPE).newInstance(this, (double) rate);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    //----------------------------------------------------------------------------------------------------

    @Override
    public void close() {
        super.close();
        ProxyEnhanceUtils.detach(getUrl().getServiceName());
    }

    //----------------------------------------------------------------------------------------------------
    public class IntCounter {
        private int count = 0;

        public IntCounter() {
        }

        public IntCounter(int count) {
            this.count = count;
        }

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }
}
