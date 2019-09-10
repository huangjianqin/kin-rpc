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
            paramBody.add(org.kin.framework.utils.ClassUtils.primitiveUnpackage(paramTypes[i], "params[" + i + "]"));
        }

        oneLineCode.append(paramBody.toString());
        oneLineCode.append(")");

        invokeCode.append(org.kin.framework.utils.ClassUtils.primitivePackage(returnType, oneLineCode.toString()));
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
