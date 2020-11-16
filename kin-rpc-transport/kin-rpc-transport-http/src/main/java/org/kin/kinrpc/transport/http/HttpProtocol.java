package org.kin.kinrpc.transport.http;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.kin.framework.utils.JSON;
import org.kin.kinrpc.rpc.*;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.transport.Protocol;
import org.springframework.remoting.support.RemoteInvocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class HttpProtocol implements Protocol {
    /**
     *
     */
    private final Map<String, JsonRpcServer> skeletonMap = new ConcurrentHashMap<>();
    /** http server 缓存 */
    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<>();
    /** 通过spi加载对应http server binder */
    private HttpBinder httpBinder = RpcServiceLoader.LOADER.getAdaptiveExtension(HttpBinder.class);


    @Override
    public int getDefaultPort() {
        return 80;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) {
        Url url = invoker.url();
        String addr = url.getAddress();
        HttpServer httpServer = serverMap.get(addr);
        if (httpServer == null) {
            httpServer = httpBinder.bind(url, new InternalHandler(false));
            serverMap.put(addr, httpServer);
        }
        String path = url.getPath();
        String genericPath = path + "/" + Constants.GENERIC_KEY;
        JsonRpcServer skeleton = new JsonRpcServer(JSON.PARSER, invoker, invoker.getInterface());
        JsonRpcServer genericServer = new JsonRpcServer(JSON.PARSER, invoker, GenericRpcService.class);
        skeletonMap.put(path, skeleton);
        skeletonMap.put(genericPath, genericServer);
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
                Class<?> interfaceC = proxy.getClass();
                Class<?>[] classes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    classes[i] = params[i].getClass();
                }
                Method method = interfaceC.getMethod(methodName, classes);
                return method.invoke(proxy, params);
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
