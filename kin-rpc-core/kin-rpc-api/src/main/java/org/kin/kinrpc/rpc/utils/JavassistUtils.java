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

    public static String primitivePackage(Class claxx, String code) {
        StringBuffer sb = new StringBuffer();
        /** 需要手动装箱, 不然编译会报错 */
        if (claxx.isPrimitive()) {
            if (Integer.TYPE.equals(claxx)) {
                sb.append("Integer.valueOf(");
            } else if (Short.TYPE.equals(claxx)) {
                sb.append("Short.valueOf(");
            } else if (Byte.TYPE.equals(claxx)) {
                sb.append("Byte.valueOf(");
            } else if (Long.TYPE.equals(claxx)) {
                sb.append("Long.valueOf(");
            } else if (Float.TYPE.equals(claxx)) {
                sb.append("Float.valueOf(");
            } else if (Double.TYPE.equals(claxx)) {
                sb.append("Double.valueOf(");
            } else if (Character.TYPE.equals(claxx)) {
                sb.append("Character.valueOf(");
            }
        }
        sb.append(code);
        if (claxx.isPrimitive() && !Void.TYPE.equals(claxx)) {
            sb.append(")");
        }
        return sb.toString();
    }

    public static String primitiveUnpackage(Class claxx, String code) {
        /** 需要手动拆箱, 不然编译会报错 */
        if (Integer.TYPE.equals(claxx)) {
            return "((" + Integer.class.getSimpleName() + ")" + code + ").intValue()";
        } else if (Short.TYPE.equals(claxx)) {
            return "((" + Short.class.getSimpleName() + ")" + code + ").shortValue()";
        } else if (Byte.TYPE.equals(claxx)) {
            return "((" + Byte.class.getSimpleName() + ")" + code + ").byteValue()";
        } else if (Long.TYPE.equals(claxx)) {
            return "((" + Long.class.getSimpleName() + ")" + code + ").longValue()";
        } else if (Float.TYPE.equals(claxx)) {
            return "((" + Float.class.getSimpleName() + ")" + code + ").floatValue()";
        } else if (Double.TYPE.equals(claxx)) {
            return "((" + Double.class.getSimpleName() + ")" + code + ").doubleValue()";
        } else if (Character.TYPE.equals(claxx)) {
            return "((" + Character.class.getSimpleName() + ")" + code + ").charValue()";
        } else if (!Void.TYPE.equals(claxx)) {
            return "(" + claxx.getName() + ")" + code;
        }
        return code;
    }

    private static String generateProviderInvokeCode(String fieldName, Method method) {
        StringBuffer invokeCode = new StringBuffer();

        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(Void.TYPE)) {
            invokeCode.append("result = ");
        }

        StringBuffer oneLineCode = new StringBuffer();
        oneLineCode.append(fieldName + "." + method.getName() + "(");

        Class[] paramTypes = method.getParameterTypes();
        StringJoiner paramBody = new StringJoiner(", ");
        for (int i = 0; i < paramTypes.length; i++) {
            paramBody.add(primitiveUnpackage(paramTypes[i], "params[" + i + "]"));
        }

        oneLineCode.append(paramBody.toString());
        oneLineCode.append(")");

        invokeCode.append(primitivePackage(returnType, oneLineCode.toString()));
        invokeCode.append(";");

        return invokeCode.toString();
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
                proxyClass.addInterface(pool.getCtClass(JavassistMethodInvoker.class.getName()));

                //添加成员域
                String fieldName = "service";
                CtField serviceField = new CtField(pool.get(interfaceClass.getName()), fieldName, proxyClass);
                serviceField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                proxyClass.addField(serviceField);

                //处理构造方法
                CtConstructor constructor = new CtConstructor(new CtClass[]{pool.get(interfaceClass.getName())}, proxyClass);
                constructor.setBody("{$0." + fieldName + " = $1;}");
                proxyClass.addConstructor(constructor);

                //方法体
                StringBuffer methodBody = new StringBuffer();
                methodBody.append(METHOD_SIGNATURE + "{");
                methodBody.append("Object result = null;");
                methodBody.append(generateProviderInvokeCode(fieldName, method));
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
            CTCLASS_CACHE.removeAll(serviceName);
        }
    }

    public static void cacheCTClass(String serviceName, CtClass ctClass) {
        CTCLASS_CACHE.put(serviceName, ctClass);
    }


    public static ClassPool getPool() {
        return pool;
    }
}
