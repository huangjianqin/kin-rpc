package org.kin.kinrpc.cluster;

import javassist.*;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.utils.ClassUtils;
import org.kin.kinrpc.rpc.utils.JavassistUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
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

    public T proxy() {
        ClassPool classPool = JavassistUtils.getPool();
        CtClass proxyClass = classPool.makeClass("org.kin.kinrpc.cluster." +
                interfaceClass.getSimpleName() + "$JavassistProxy");
        try {
            //接口
            proxyClass.addInterface(classPool.get(interfaceClass.getName()));

            CtField invokerField = new CtField(classPool.get(this.getClass().getName()), "invoker", proxyClass);
            invokerField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyClass.addField(invokerField);

            //构造器
            CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(this.getClass().getName())}, proxyClass);
            constructor.setBody("{$0.invoker = $1;}");
            proxyClass.addConstructor(constructor);

            int internalClassNum = 0;
            //生成接口方法方法体
            for (Method method : interfaceClass.getDeclaredMethods()) {
                StringBuffer sb = new StringBuffer();
                sb.append("public ");
                sb.append(method.getReturnType().getName() + " ");
                sb.append(method.getName() + "(");

                StringJoiner paramBody = new StringJoiner(", ");

                Class[] parameterTypes = method.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    paramBody.add(parameterTypes[i].getName() + " " + "arg" + i);
                }
                sb.append(paramBody.toString() + "){");

                //真正逻辑
                StringBuffer methodBody = new StringBuffer();
                Class returnType = method.getReturnType();
                boolean isVoid = Void.class.equals(returnType) || Void.TYPE.equals(returnType);
                if (!isVoid) {
                    methodBody.append("return ");
                    if (returnType.isPrimitive()) {
                        /** 需要手动拆箱, 不然编译会报错 */
                        if (Integer.TYPE.equals(returnType)) {
                            methodBody.append("((" + Integer.class.getSimpleName() + ")");
                        } else if (Short.TYPE.equals(returnType)) {
                            methodBody.append("((" + Short.class.getSimpleName() + ")");
                        } else if (Byte.TYPE.equals(returnType)) {
                            methodBody.append("((" + Byte.class.getSimpleName() + ")");
                        } else if (Long.TYPE.equals(returnType)) {
                            methodBody.append("((" + Long.class.getSimpleName() + ")");
                        } else if (Float.TYPE.equals(returnType)) {
                            methodBody.append("((" + Float.class.getSimpleName() + ")");
                        } else if (Double.TYPE.equals(returnType)) {
                            methodBody.append("((" + Double.class.getSimpleName() + ")");
                        } else if (Character.TYPE.equals(returnType)) {
                            methodBody.append("((" + Character.class.getSimpleName() + ")");
                        }
                    } else {
                        methodBody.append("(" + returnType.getName() + ")");
                    }
                    if (Future.class.equals(returnType)) {
                        methodBody.append("invoker.invokeAsync(\"" +
                                ClassUtils.getUniqueName(method) + "\", " +
                                isVoid);
                    } else {
                        methodBody.append("invoker.invoke0(\"" +
                                ClassUtils.getUniqueName(method) + "\", " +
                                isVoid);
                    }
                    if (parameterTypes.length > 0) {
                        methodBody.append(", new Object[]{");
                        StringJoiner invokeBody = new StringJoiner(", ");
                        for (int i = 0; i < parameterTypes.length; i++) {
                            String prefix = "";
                            if (parameterTypes[i].isPrimitive()) {
                                if (Integer.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Integer.valueOf(";
                                } else if (Short.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Short.valueOf(";
                                } else if (Byte.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Byte.valueOf(";
                                } else if (Long.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Long.valueOf(";
                                } else if (Float.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Float.valueOf(";
                                } else if (Double.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Double.valueOf(";
                                } else if (Character.TYPE.equals(parameterTypes[i])) {
                                    prefix = "Character.valueOf(";
                                }
                            }
                            String argStr = "arg" + i;
                            String suffix = "";
                            if (parameterTypes[i].isPrimitive()) {
                                suffix = ")";
                            }
                            invokeBody.add(prefix + argStr + suffix);
                        }
                        methodBody.append(invokeBody.toString());
                    } else {
                        methodBody.append(", new Object[0]");
                    }

                    if (returnType.isPrimitive()) {
                        if (parameterTypes.length > 0) {
                            methodBody.append("})");
                        } else {
                            methodBody.append(")");
                        }
                        /** 需要手动拆箱, 不然编译会报错 */
                        if (Integer.TYPE.equals(returnType)) {
                            methodBody.append(").intValue()");
                        } else if (Short.TYPE.equals(returnType)) {
                            methodBody.append(").shortValue()");
                        } else if (Byte.TYPE.equals(returnType)) {
                            methodBody.append(").byteValue()");
                        } else if (Long.TYPE.equals(returnType)) {
                            methodBody.append(").longValue()");
                        } else if (Float.TYPE.equals(returnType)) {
                            methodBody.append(").floatValue()");
                        } else if (Double.TYPE.equals(returnType)) {
                            methodBody.append(").doubleValue()");
                        } else if (Character.TYPE.equals(returnType)) {
                            methodBody.append(").charValue()");
                        }
                        methodBody.append(";");
                    } else {
                        if (parameterTypes.length > 0) {
                            methodBody.append("});");
                        } else {
                            methodBody.append(");");
                        }
                    }
                } else {
                    if (Future.class.equals(returnType)) {
                        methodBody.append("invoker.invokeAsync(\"" +
                                ClassUtils.getUniqueName(method) + "\", " +
                                isVoid);
                    } else {
                        methodBody.append("invoker.invoke0(\"" +
                                ClassUtils.getUniqueName(method) + "\", " +
                                isVoid);
                    }
                    if (parameterTypes.length > 0) {
                        methodBody.append(", new Object[]{");
                    } else {
                        methodBody.append(", new Object[0]");
                    }

                    StringJoiner invokeBody = new StringJoiner(", ");
                    for (int i = 0; i < parameterTypes.length; i++) {
                        String prefix = "";
                        if (parameterTypes[i].isPrimitive()) {
                            if (Integer.TYPE.equals(parameterTypes[i])) {
                                prefix = "Integer.valueOf(";
                            } else if (Short.TYPE.equals(parameterTypes[i])) {
                                prefix = "Short.valueOf(";
                            } else if (Byte.TYPE.equals(parameterTypes[i])) {
                                prefix = "Byte.valueOf(";
                            } else if (Long.TYPE.equals(parameterTypes[i])) {
                                prefix = "Long.valueOf(";
                            } else if (Float.TYPE.equals(parameterTypes[i])) {
                                prefix = "Float.valueOf(";
                            } else if (Double.TYPE.equals(parameterTypes[i])) {
                                prefix = "Double.valueOf(";
                            } else if (Character.TYPE.equals(parameterTypes[i])) {
                                prefix = "Character.valueOf(";
                            }
                        }
                        String argStr = "arg" + i;
                        String suffix = "";
                        if (parameterTypes[i].isPrimitive()) {
                            suffix = ")";
                        }
                        invokeBody.add(prefix + argStr + suffix);
                    }
                    methodBody.append(invokeBody.toString());
                    if (parameterTypes.length > 0) {
                        methodBody.append("});");
                    } else {
                        methodBody.append(");");
                    }
                    methodBody.append("return null;");
                }

                if (CompletableFuture.class.equals(returnType)) {
                    //需要构造内部抽象类
                    CtClass internalClass = classPool.makeClass(proxyClass.getName() + "$" + internalClassNum);
                    internalClass.addInterface(classPool.get(Supplier.class.getName()));

                    Collection<CtClass> constructArgClass = new ArrayList<>();

                    CtField internalClassField = new CtField(classPool.get(this.getClass().getName()), "invoker", internalClass);
                    internalClassField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                    internalClass.addField(internalClassField);
                    constructArgClass.add(classPool.get(this.getClass().getName()));

                    for (int i = 0; i < parameterTypes.length; i++) {
                        CtField internalClassField1 = new CtField(classPool.get(parameterTypes[i].getName()), "arg" + i, internalClass);
                        internalClassField1.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                        internalClass.addField(internalClassField1);
                        constructArgClass.add(classPool.get(parameterTypes[i].getName()));
                    }

                    //构造器
                    CtConstructor internalClassConstructor = new CtConstructor(constructArgClass.toArray(new CtClass[constructArgClass.size()]), internalClass);
                    StringBuffer constructBody = new StringBuffer();
                    constructBody.append("{");
                    constructBody.append("$0.invoker = $1;");
                    for (int i = 1; i <= parameterTypes.length; i++) {
                        constructBody.append("$0.arg" + (i - 1) + " = $" + (i + 1) + ";");
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
                    invokeParam.add("invoker");
                    for (int i = 0; i < parameterTypes.length; i++) {
                        invokeParam.add("arg" + i);
                    }

                    methodBody = new StringBuffer();
                    methodBody.append("return " + CompletableFuture.class.getName() + ".supplyAsync(new " + realInternalClass.getName() + "(" + invokeParam.toString() + "));");
                    sb.append(methodBody.toString());

                    internalClassNum++;
                } else {
                    //直接在方法体实现
                    sb.append(methodBody.toString());
                }

                sb.append("}");

                CtMethod ctMethod = CtMethod.make(sb.toString(), proxyClass);
                proxyClass.addMethod(ctMethod);
            }

            Class<T> realProxyClass = (Class<T>) proxyClass.toClass();
            T proxy = realProxyClass.getConstructor(this.getClass()).newInstance(this);
            JavassistUtils.cacheCTClass(getUrl().getServiceName(), proxyClass);
            return proxy;
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    @Override
    public void close() {
        super.close();
        JavassistUtils.detach(getUrl().getServiceName());
    }
}
