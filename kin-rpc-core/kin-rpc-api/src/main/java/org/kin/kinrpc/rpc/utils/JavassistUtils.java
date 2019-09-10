package org.kin.kinrpc.rpc.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.invoker.JavassistMethodInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author huangjianqin
 * @date 2019-09-09
 */
public class JavassistUtils {
    private static final Logger log = LoggerFactory.getLogger(JavassistUtils.class);
    private static final ClassPool pool = ClassPool.getDefault();
    private static Multimap<String, CtClass> CTCLASS_CACHE = HashMultimap.create();

    private static final String JAVASSIST_PROXY_PACKAGE = "org.kin.kinrpc.rpc.invoker.proxy.";
    private static final String METHOD_SIGNATURE = "public Object invoke(Object[] params) throws Exception";

    private JavassistUtils() {
    }

    /**
     * 利用javassist字节码技术生成方法代理类, 调用效率比反射要高
     */
    public static Map<String, JavassistMethodInvoker> generateProviderMethodProxy(Object service, Class interfaceClass, String serviceName) {
        Map<String, JavassistMethodInvoker> result = new HashMap<>();
        for (Method method : interfaceClass.getDeclaredMethods()) {
            String uniqueName = ClassUtils.getUniqueName(method);

            //打印日志信息
            Class<?>[] logParamTypes = method.getParameterTypes();
            String[] paramTypeStrs = new String[logParamTypes.length];
            for (int i = 0; i < paramTypeStrs.length; i++) {
                paramTypeStrs[i] = logParamTypes[i].getName();
            }
            log.debug("'{}' method's params' type", uniqueName);
            log.debug(StringUtils.mkString(paramTypeStrs));


            String className = JAVASSIST_PROXY_PACKAGE + interfaceClass.getSimpleName() + "$" + uniqueName + "JavassistProxy";
            CtClass proxyClass = pool.makeClass(className);

            try {
                //实现接口
                proxyClass.setInterfaces(new CtClass[]{pool.getCtClass(JavassistMethodInvoker.class.getName())});

                //添加成员域
                CtField serviceField = new CtField(pool.get(interfaceClass.getName()), "service", proxyClass);
                serviceField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                proxyClass.addField(serviceField);

                //处理构造方法
                CtConstructor constructor = new CtConstructor(new CtClass[]{pool.get(interfaceClass.getName())}, proxyClass);
                constructor.setBody("{$0.service = $1;}");
                proxyClass.addConstructor(constructor);

                //方法体
                StringBuffer methodBody = new StringBuffer();
                methodBody.append(METHOD_SIGNATURE + "{");
                methodBody.append("Object result = null;");

                Class<?> returnType = method.getReturnType();
                if (!returnType.equals(Void.TYPE)) {
                    methodBody.append("result = ");
                    /** 需要手动装箱, 不然编译会报错 */
                    if (returnType.isPrimitive()) {
                        if (Integer.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Integer.valueOf(");
                        } else if (Short.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Short.valueOf(");
                        } else if (Byte.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Byte.valueOf(");
                        } else if (Long.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Long.valueOf(");
                        } else if (Float.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Float.valueOf(");
                        } else if (Double.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Double.valueOf(");
                        } else if (Character.TYPE.equals(method.getReturnType())) {
                            methodBody.append("Character.valueOf(");
                        }
                    }
                }
                methodBody.append("service." + method.getName() + "(");

                Class[] paramTypes = method.getParameterTypes();
                StringJoiner paramBody = new StringJoiner(", ");
                for (int i = 0; i < paramTypes.length; i++) {
                    if (paramTypes[i].isPrimitive()) {
                        /** 需要手动拆箱, 不然编译会报错 */
                        if (Integer.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Integer.class.getSimpleName() + ")params[" + i + "]).intValue()");
                        } else if (Short.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Short.class.getSimpleName() + ")params[" + i + "]).shortValue()");
                        } else if (Byte.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Byte.class.getSimpleName() + ")params[" + i + "]).byteValue()");
                        } else if (Long.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Long.class.getSimpleName() + ")params[" + i + "]).longValue()");
                        } else if (Float.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Float.class.getSimpleName() + ")params[" + i + "]).floatValue()");
                        } else if (Double.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Double.class.getSimpleName() + ")params[" + i + "]).doubleValue()");
                        } else if (Character.TYPE.equals(paramTypes[i])) {
                            paramBody.add("((" + Character.class.getSimpleName() + ")params[" + i + "]).charValue()");
                        }
                    } else {
                        paramBody.add("(" + paramTypes[i].getName() + ")params[" + i + "]");
                    }
                }

                methodBody.append(paramBody.toString());
                methodBody.append(")");
                if (!method.getReturnType().equals(Void.TYPE) && returnType.isPrimitive()) {
                    methodBody.append(");");
                } else {
                    methodBody.append(";");
                }
                methodBody.append("return result; }");

                CtMethod proxyMethod = CtMethod.make(methodBody.toString(), proxyClass);
                proxyClass.addMethod(proxyMethod);

                Class<?> realProxyClass = proxyClass.toClass();
                JavassistMethodInvoker proxyMethodInvoker = (JavassistMethodInvoker) realProxyClass.getConstructor(interfaceClass).newInstance(service);

                result.put(uniqueName, proxyMethodInvoker);
                cacheCTClass(serviceName, proxyClass);
            } catch (Exception e) {
                log.error(method.toString(), e);
            }

        }
        return result;
    }

    /**
     * 尝试释放${@link ClassPool}无用空间
     */
    public static void detach(String serviceName) {
        if (CTCLASS_CACHE.containsKey(serviceName)) {
            for (CtClass ctClass : CTCLASS_CACHE.get(serviceName)) {
                ctClass.detach();
            }
        }
    }

    public static void cacheCTClass(String serviceName, CtClass ctClass){
        CTCLASS_CACHE.put(serviceName, ctClass);
    }


    public static ClassPool getPool() {
        return pool;
    }
}
