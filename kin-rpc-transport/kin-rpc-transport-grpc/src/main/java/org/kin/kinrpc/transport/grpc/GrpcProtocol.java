package org.kin.kinrpc.transport.grpc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import javassist.*;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.utils.ClassUtils;
import org.kin.kinrpc.AbstractProxyProtocol;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;

/**
 * 开发流程与grpc类似(编辑.proto, 并编译生成servcie base类), 只是底层通信和编程模式使用的是kinrpc
 *
 * @author huangjianqin
 * @date 2020/12/1
 */
public class GrpcProtocol extends AbstractProxyProtocol {
    private static final Cache<String, GrpcServer> servers = CacheBuilder.newBuilder().build();

    @Override
    protected <T> Runnable doExport(T impl, Class<T> interfaceC, Url url) {
        String address = url.getAddress();
        //grpc server
        GrpcServer grpcServer;
        try {
            grpcServer = servers.get(address, () -> {
                GrpcHandlerRegistry registry = new GrpcHandlerRegistry();

                NettyServerBuilder builder =
                        NettyServerBuilder
                                .forPort(url.getPort())
                                .fallbackHandlerRegistry(registry);
                Server server = builder.build();
                return new GrpcServer(server, registry);
            });
        } catch (ExecutionException e) {
            throw new RpcCallErrorException(e);
        }
        BindableService bindableService;
        try {
            bindableService = generateBindableService(impl, interfaceC, url.getServiceName());
        } catch (Exception e) {
            throw new RpcCallErrorException(e);
        }
        grpcServer.registry.addService(bindableService, url.getServiceName());

        if (!grpcServer.started) {
            grpcServer.start();
        }

        return () -> grpcServer.registry.removeService(url.getServiceName());
    }

    @Override
    protected <T> T doReference(Class<T> interfaceC, Url url) {
        return null;
    }

    @Override
    public void destroy() {

    }

    //------------------------------------------------------------------------------------------------------------------------

    /**
     * grpc server数据封装
     */
    private class GrpcServer {
        /** grpc server */
        private final Server server;
        /** grpc 服务方法注册 */
        private final GrpcHandlerRegistry registry;
        /** server started标识 */
        private volatile boolean started;

        public GrpcServer(Server server, GrpcHandlerRegistry registry) {
            this.server = server;
            this.registry = registry;
        }

        /**
         * start
         */
        public void start() {
            try {
                started = true;
                server.start();
            } catch (IOException e) {
                throw new RpcCallErrorException(e);
            }
        }

        /**
         * shutdown
         */
        public void close() {
            //todo
            server.shutdown();
        }
    }

    /**
     * 生成grpc所需的{@link BindableService}
     */
    private BindableService generateBindableService(Object impl, Class<?> interfaceC, String serviceName) throws NotFoundException, CannotCompileException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        ClassPool pool = ProxyEnhanceUtils.getPool();
        String ctClassName = serviceName.concat("ImplBase$Javassist");

        CtClass proxyCtClass = pool.getOrNull(ctClassName);
        if (Objects.isNull(proxyCtClass)) {
            proxyCtClass = pool.makeClass(ctClassName);

            //实现接口
            proxyCtClass.addInterface(pool.get(BindableService.class.getName()));
            proxyCtClass.addInterface(pool.get(interfaceC.getName()));

            //代理类成员域
            CtField proxyField = new CtField(pool.get(interfaceC.getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, proxyCtClass);
            proxyField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyField);

            //构造器
            CtConstructor constructor = new CtConstructor(
                    new CtClass[]{pool.get(interfaceC.getName())}, proxyCtClass);
            constructor.setBody("{$0.".concat(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).concat(" = $1;}"));
            proxyCtClass.addConstructor(constructor);

            //生成接口方法方法体
            Method[] interfaceCMethods = interfaceC.getDeclaredMethods();
            for (Method method : interfaceCMethods) {
                CtMethod ctMethod = CtMethod.make(
                        ClassUtils.generateMethodContent(
                                method,
                                ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME.concat(generateMethodInvokeStr(method))),
                        proxyCtClass);
                proxyCtClass.addMethod(ctMethod);
            }

            /**
             * 生成{@link BindableService}方法方法体
             * 目前只有一个方法
             */
            Method bSMethod = BindableService.class.getDeclaredMethods()[0];
            StringBuilder bSMethodBody = new StringBuilder();
            bSMethodBody.append("return ").append(ServerServiceDefinition.class.getName())
                    .append(".builder(")
                    .append(serviceName)
                    .append(")").append(System.lineSeparator());
            //.proto编译后的以Grpc结尾的类名
            String grpcClassName = interfaceC.getPackage().getName().concat(interfaceC.getName()).concat("Grpc");
            for (int i = 0; i < interfaceCMethods.length; i++) {
                Method method = interfaceCMethods[i];
                String methodName = method.getName();
                String methodIdStaticFieldName = "METHODID_".concat(Grpcs.methodNameUpperUnderscore(methodName));

                CtField methodIdStaticField = new CtField(pool.get(Integer.TYPE.getName()), methodIdStaticFieldName, proxyCtClass);
                methodIdStaticField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
                proxyCtClass.addField(methodIdStaticField);

                StringBuilder sb = new StringBuilder();
                sb.append(".addMethod(");
                sb.append(grpcClassName)
                        .append(".get")
                        .append(Grpcs.methodNamePascalCase(methodName))
                        .append("Method()")
                        .append(",")
                        //todo
                        .append(methodName)
                        .append("(new MethodHandlers(")
                        .append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME)
                        .append(",")
                        .append(methodIdStaticField)
                        .append(")))")
                        .append(System.lineSeparator());
                bSMethodBody.append(sb.toString());

                //添加MethodHandlers内部类
                generateMethodHandlersClass(pool, ctClassName, interfaceC, method, i, methodIdStaticFieldName);
            }
            bSMethodBody.append(".build();");
            CtMethod ctMethod = CtMethod.make(
                    ClassUtils.generateMethodContent(
                            bSMethod,
                            bSMethodBody.toString()),
                    proxyCtClass);
            proxyCtClass.addMethod(ctMethod);

            ProxyEnhanceUtils.cacheCTClass(ctClassName, proxyCtClass);
        }
        Class<?> proxyClass = proxyCtClass.toClass();
        return (BindableService) proxyClass.getConstructor(interfaceC).newInstance(impl);
    }

    /**
     * 生成.XXX(X,X,X);
     */
    private String generateMethodInvokeStr(Method method) {
        StringBuilder invokeCode = new StringBuilder();
        invokeCode.append(".").append(method.getName()).append("(\"");

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            StringJoiner invokeBody = new StringJoiner(", ");
            for (int i = 0; i < parameterTypes.length; i++) {
                invokeBody.add("arg".concat(Integer.toString(i)));
            }
            invokeCode.append(invokeBody.toString());
        }
        invokeCode.append(");");
        return invokeCode.toString();
    }

    /**
     * 添加MethodHandlers内部类
     *
     * @return MethodHandlers内部类名
     */
    private String generateMethodHandlersClass(ClassPool pool, String ctClassName, Class<?> interfaceC, Method method, int id, String methodIdFieldStaticName) throws NotFoundException, CannotCompileException {
        //添加MethodHandlers内部类
        String mhClassName = ctClassName.concat("$MethodHandlers").concat(Integer.toString(id));
        CtClass mhCtClass = pool.makeClass(mhClassName);

        mhCtClass.addInterface(pool.get(ServerCalls.UnaryMethod.class.getName()));
        mhCtClass.addInterface(pool.get(ServerCalls.ServerStreamingMethod.class.getName()));
        mhCtClass.addInterface(pool.get(ServerCalls.ClientStreamingMethod.class.getName()));
        mhCtClass.addInterface(pool.get(ServerCalls.BidiStreamingMethod.class.getName()));

        //代理类成员域
        CtField inProxyField = new CtField(pool.get(interfaceC.getName()), ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME, mhCtClass);
        inProxyField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        mhCtClass.addField(inProxyField);

        String methodIdFieldName = "methodId";
        CtField methodIdField = new CtField(pool.get(Integer.TYPE.getName()), methodIdFieldName, mhCtClass);
        methodIdField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        mhCtClass.addField(inProxyField);

        //构造器
        CtConstructor constructor = new CtConstructor(
                new CtClass[]{pool.get(interfaceC.getName()), pool.get(Integer.TYPE.getName())}, mhCtClass);
        StringBuilder constructorBody = new StringBuilder();
        constructorBody.append("{$0.").append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME).append(" = $1;").append(System.lineSeparator());
        constructorBody.append("{$0.").append(methodIdFieldName).append(" = $2;}").append(System.lineSeparator());
        constructor.setBody(constructorBody.toString());
        mhCtClass.addConstructor(constructor);

        StringBuilder methodBody1 = new StringBuilder();
        methodBody1.append("public void invoke(Object request, io.grpc.stub.StreamObserver responseObserver){").append(System.lineSeparator());
        methodBody1.append("switch(").append(methodIdFieldName)
                .append(") {").append(System.lineSeparator());
        methodBody1.append("case ").append(methodIdFieldStaticName)
                .append(":").append(System.lineSeparator());
        methodBody1.append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME)
                .append(".").append(method.getName()).append("(request, responseObserver);break;")
                .append("default: throw new java.lang.AssertionError();");
        methodBody1.append("}").append(System.lineSeparator());
        methodBody1.append("}").append(System.lineSeparator());
        mhCtClass.addMethod(CtMethod.make(methodBody1.toString(), mhCtClass));

        StringBuilder methodBody2 = new StringBuilder();
        methodBody2.append("public io.grpc.stub.StreamObserver invoke(io.grpc.stub.StreamObserver responseObserver){").append(System.lineSeparator());
        methodBody2.append("switch(").append(methodIdFieldName)
                .append(") {").append(System.lineSeparator());
        methodBody2.append("case ").append(methodIdFieldStaticName)
                .append(":").append(System.lineSeparator());
        methodBody2.append("return (io.grpc.stub.StreamObserver) ").append(ProxyEnhanceUtils.DEFAULT_PROXY_FIELD_NAME)
                .append(".").append(method.getName()).append("(responseObserver);")
                .append("default: throw new java.lang.AssertionError();");
        methodBody2.append("}").append(System.lineSeparator());
        methodBody2.append("}").append(System.lineSeparator());
        mhCtClass.addMethod(CtMethod.make(methodBody2.toString(), mhCtClass));

        return mhClassName;
    }
}
