package org.kin.kinrpc.transport.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.JSON;
import org.kin.kinrpc.rpc.*;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.transport.Protocol;
import org.kin.kinrpc.transport.http.tomcat.TomcatHttpBinder;
import org.springframework.remoting.support.RemoteInvocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class HttpProtocol implements Protocol, LoggerOprs {
    static {
        ObjectMapper objectMapper = JSON.PARSER;
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     *
     */
    private final Map<String, JsonRpcServer> skeletonMap = new ConcurrentHashMap<>();
    /** http server 缓存 */
    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<>();
    /** 通过spi加载对应http server binder */
    private HttpBinder httpBinder;

    public HttpProtocol() {
        httpBinder = RpcServiceLoader.LOADER.getAdaptiveExtension(HttpBinder.class);
        if (Objects.isNull(httpBinder)) {
            httpBinder = new TomcatHttpBinder();
        }
    }

    @Override
    public int getDefaultPort() {
        return 80;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) {
        Url url = invoker.url();
        String addr = url.getAddress();
        HttpServer httpServer = serverMap.get(addr);
        if (Objects.isNull(httpServer)) {
            httpServer = httpBinder.bind(url, new InternalHandler(false));
            serverMap.put(addr, httpServer);
        }

        Class<T> interfaceC = invoker.getInterface();
        T proxy = (T) Proxy.newProxyInstance(interfaceC.getClassLoader(), new Class<?>[]{interfaceC}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return invoker.invoke(ClassUtils.getUniqueName(method), args);
            }
        });

        String path = url.getPath();
        String genericPath = path + "/" + Constants.GENERIC_KEY;
        JsonRpcServer skeleton = new JsonRpcServer(JSON.PARSER, proxy, interfaceC);
        JsonRpcServer genericServer = new JsonRpcServer(JSON.PARSER, proxy, GenericRpcService.class);
        skeletonMap.put(path, skeleton);
        skeletonMap.put(genericPath, genericServer);

        info("service '{}' export path '{}'", url.str(), path);
        info("service '{}' export path '{}'", url.str(), genericPath);

        return new Exporter<T>() {
            @Override
            public Invoker<T> getInvoker() {
                return invoker;
            }

            @Override
            public void unexport() {
                skeletonMap.remove(path);
                skeletonMap.remove(genericPath);
            }
        };
    }

    @Override
    public <T> AsyncInvoker<T> reference(Url url) {
//        final String generic = url.getParameter(Constants.GENERIC_KEY);
//        final boolean isGeneric = ProtocolUtils.isGeneric(generic) || serviceType.equals(GenericService.class);
        JsonProxyFactoryBean jsonProxyFactoryBean = new JsonProxyFactoryBean();
        JsonRpcProxyFactoryBean jsonRpcProxyFactoryBean = new JsonRpcProxyFactoryBean(jsonProxyFactoryBean);
        jsonRpcProxyFactoryBean.setRemoteInvocationFactory((methodInvocation) -> {
            RemoteInvocation invocation = new JsonRemoteInvocation(methodInvocation);
//            if (isGeneric) {
//                invocation.addAttribute(Constants.GENERIC_KEY, generic);
//            }
            return invocation;
        });
        String key = url.identityStr();
//        if (isGeneric) {
//            key = key + "/" + Constants.GENERIC_KEY;
//        }

        jsonRpcProxyFactoryBean.setServiceUrl(key);
        try {
            jsonRpcProxyFactoryBean.setServiceInterface(Class.forName(url.getInterfaceN()));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        jsonProxyFactoryBean.afterPropertiesSet();
        T proxy = (T) jsonProxyFactoryBean.getObject();
        return new ReferenceInvoker<T>(url) {

            @Override
            public Object invoke(String methodName, Object... params) throws Throwable {
                //todo 临时先用反射
                Class<?> interfaceC = Class.forName(url.getInterfaceN());
                Class<?>[] classes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    classes[i] = params[i].getClass();
                }

                Method targetMethod = null;
                for (Method method : interfaceC.getMethods()) {
                    //todo 临时遍历所有方法, 用方法名判断
                    if (methodName.equals(ClassUtils.getUniqueName(method))) {
                        targetMethod = method;
                        break;
                    }
                }

                Preconditions.checkNotNull(targetMethod, String.format("can't not find method '%s'", methodName));

                return targetMethod.invoke(proxy, params);
            }

            @Override
            public Future<Object> invokeAsync(String methodName, Object... params) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return invoke(methodName, params);
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }, RpcThreadPool.EXECUTORS);
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public void destroy() {

            }
        };
    }

    @Override
    public void destroy() {
        for (HttpServer httpServer : serverMap.values()) {
            httpServer.close();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * http handler
     */
    private class InternalHandler implements HttpHandler {
        private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
        private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
        private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

        private boolean cors;

        public InternalHandler(boolean cors) {
            this.cors = cors;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws ServletException {
            String uri = request.getRequestURI();
            //去掉开头的/
            uri = uri.substring(1);
            JsonRpcServer skeleton = skeletonMap.get(uri);
            if (cors) {
                response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
                response.setHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST");
                response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS_HEADER, "*");
            }
            if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
                response.setStatus(200);
            } else if (request.getMethod().equalsIgnoreCase("POST")) {
                try {
                    skeleton.handle(request, response);
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            } else {
                response.setStatus(500);
            }
        }

    }
}
