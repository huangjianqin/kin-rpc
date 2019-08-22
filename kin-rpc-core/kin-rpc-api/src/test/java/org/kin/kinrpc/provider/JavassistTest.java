package org.kin.kinrpc.provider;

import javassist.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019-08-21
 */
@BenchmarkMode(Mode.Throughput)//基准测试类型
@OutputTimeUnit(TimeUnit.SECONDS)//基准测试结果的时间类型
@Warmup(iterations = 5)//预热的迭代次数
@Threads(2)//测试线程数量
@State(Scope.Thread)//该状态为每个线程独享
//度量:iterations进行测试的轮次，time每轮进行的时长，timeUnit时长单位,batchSize批次数量
@Measurement(iterations = 100000, time = 30, timeUnit = TimeUnit.SECONDS, batchSize = 5)
public class JavassistTest {
    private static Sayable instance = new SayImpl();
    private static Sayable jdkProxy = (Sayable) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class[]{Sayable.class}, new MyInvocation(instance));
    private static Method method;

    static {
        try {
            method = Sayable.class.getMethod("say");
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    private static Invoker proxyInstance;
    static {
        Class interfaceClass = Sayable.class;

        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.makeClass(interfaceClass.getName() + "$JavassistProxy");
        try {
//            cc.addInterface(pool.get(interfaceClass.getName()));
            cc.addInterface(pool.get(Invoker.class.getName()));
            cc.addConstructor(CtNewConstructor.defaultConstructor(cc));
            String proxyFieldName = "proxy";
            cc.addField(CtField.make("private " + interfaceClass.getName() + " " + proxyFieldName + ";", cc));

            //handle method
//            for (Method method : interfaceClass.getMethods()) {
//                StringBuffer sb = new StringBuffer();
//                sb.append("public ");
//                sb.append(method.getReturnType().getName() + " ");
//                sb.append(method.getName() + "(");
//                for (int i = 0; i < method.getParameterTypes().length; i++) {
//                    Class paramType = method.getParameterTypes()[i];
//                    sb.append(paramType.getName() + " arg" + i);
//                    if(i != method.getParameterTypes().length - 1){
//                        sb.append(", ");
//                    }
//                }
//                sb.append("){ " + proxyFieldName + "." + method.getName() + "(");
//                for (int i = 0; i < method.getParameterTypes().length; i++) {
//                    sb.append("arg" + i);
//                    if(i != method.getParameterTypes().length - 1){
//                        sb.append(", ");
//                    }
//                }
//                sb.append("); }");
//                cc.addMethod(CtNewMethod.make(sb.toString(), cc));
//            }
            cc.addMethod(CtNewMethod.make("public void invoke() { " + proxyFieldName + ".say(); }", cc));

            Class<?> proxyClazz = SingleClassLoader.loadClass(instance.getClass().getClassLoader(), cc.toBytecode());
            Object proxyInstance = proxyClazz.newInstance();
            Field proxyField = proxyClazz.getDeclaredField(proxyFieldName);
            proxyField.setAccessible(true);
            proxyField.set(proxyInstance, instance);
            proxyField.setAccessible(false);

            JavassistTest.proxyInstance = (Invoker) proxyInstance;
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RunnerException, InterruptedException {
        Options opt = new OptionsBuilder()
                .include(JavassistTest.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    private static class MyInvocation implements InvocationHandler{
        private Sayable proxy;

        public MyInvocation(Sayable proxy) {
            this.proxy = proxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(this.proxy, args);
        }
    }

    @Benchmark
    public static void reflection() {
        try {
            method.invoke(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    public static void javassist() {
        proxyInstance.invoke();
    }

    public static class SingleClassLoader extends ClassLoader {

        private final Class<?> clazz;

        public SingleClassLoader(ClassLoader parent, byte[] bytes) {
            super(parent);
            this.clazz = defineClass(null, bytes, 0, bytes.length, null);
        }

        public Class<?> getClazz() {
            return clazz;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (clazz != null && clazz.getName().equals(name)) {
                return clazz;
            }

            return getParent().loadClass(name);
        }

        public static <T> Class<T> loadClass(byte[] bytes) {
            ClassLoader parent = SingleClassLoader.class.getClassLoader();
            return loadClass(parent, bytes);
        }

        @SuppressWarnings("unchecked")
        public static <T> Class<T> loadClass(ClassLoader parent, byte[] bytes) {
            return (Class<T>) new SingleClassLoader(parent, bytes).getClazz();
        }
    }
}
