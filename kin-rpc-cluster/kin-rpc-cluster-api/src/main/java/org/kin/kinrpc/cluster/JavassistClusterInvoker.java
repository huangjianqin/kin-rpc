package org.kin.kinrpc.cluster;

import javassist.*;
import org.kin.framework.math.IntCounter;
import org.kin.framework.proxy.utils.ProxyEnhanceUtils;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.common.URL;

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
class JavassistClusterInvoker<T> extends ClusterInvoker {
    private Class<T> interfaceClass;

    public JavassistClusterInvoker(Cluster cluster, int retryTimes, int retryTimeout, URL url, Class<T> interfaceClass) {
        super(cluster, retryTimes, retryTimeout, url);
        this.interfaceClass = interfaceClass;
    }

    public static <T> T proxy(Cluster cluster, int retryTimes, int retryTimeout, URL url, Class<T> interfaceClass) {
        return new JavassistClusterInvoker<T>(cluster, retryTimes, retryTimeout, url, interfaceClass).proxy();
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

        StringBuffer invokeCode = new StringBuffer();
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
                    constructArgClass.toArray(new CtClass[constructArgClass.size()]), internalClass);
            StringBuffer constructBody = new StringBuffer();
            constructBody.append("{");
            constructBody.append("$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat("= $1;"));
            for (int i = 1; i <= parameterTypes.length; i++) {
                constructBody.append("$0.arg".concat(Integer.toString((i - 1))).concat(" = $").concat(Integer.toString((i + 1))).concat(";"));
            }
            constructBody.append("}");
            internalClassConstructor.setBody(constructBody.toString());
            internalClass.addConstructor(internalClassConstructor);

            StringBuffer internalMethodBody = new StringBuffer();
            internalMethodBody.append("public Object get(){");
            internalMethodBody.append(methodBody.toString());
            internalMethodBody.append("}");

            CtMethod internalMethod = CtMethod.make(internalMethodBody.toString(), internalClass);
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

                    //构造器
                    CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(this.getClass().getName())}, proxyClass);
                    constructor.setBody("{$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat(" = $1;}"));
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
                        sb.append(generateMethodBody(classPool, proxyClass, method, parameterTypes, internalClassNum));
                        sb.append("}");

                        CtMethod ctMethod = CtMethod.make(sb.toString(), proxyClass);
                        proxyClass.addMethod(ctMethod);
                    }
                    ProxyEnhanceUtils.cacheCTClass(getUrl().getServiceName(), proxyClass);
                }

                realProxyClass = (Class<T>) proxyClass.toClass();
            }
            T proxy = realProxyClass.getConstructor(this.getClass()).newInstance(this);
            return proxy;
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
}
