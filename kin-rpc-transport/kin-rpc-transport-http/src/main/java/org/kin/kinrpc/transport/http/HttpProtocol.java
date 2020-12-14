package org.kin.kinrpc.transport.http;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.kin.framework.utils.JSON;
import org.kin.kinrpc.rpc.GenericRpcService;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.AbstractProxyProtocol;
import org.kin.kinrpc.transport.http.tomcat.TomcatHttpBinder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public final class HttpProtocol extends AbstractProxyProtocol {
    static {
        ObjectMapper objectMapper = JSON.PARSER;
        //带上类型信息
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        //允许未知属性
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //允许空bean
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /** JsonRpcServer缓存 */
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
    protected <T> Runnable doExport(T proxyedInvoker, Class<T> interfaceC, Url url) {
        String addr = url.getAddress();
        HttpServer httpServer = serverMap.get(addr);
        if (Objects.isNull(httpServer)) {
            //启动http server
            httpServer = httpBinder.bind(url, new InternalHandler(false));
            serverMap.put(addr, httpServer);
        }

        //普通url path
        String path = url.getPath();
        //使用generic url path
        String genericPath = path + "/" + Constants.GENERIC;
        JsonRpcServer skeleton = new JsonRpcServer(JSON.PARSER, proxyedInvoker, interfaceC);
        JsonRpcServer genericServer = new JsonRpcServer(JSON.PARSER, proxyedInvoker, GenericRpcService.class);
        skeletonMap.put(path, skeleton);
        skeletonMap.put(genericPath, genericServer);

        info("http service '{}' export path '{}'", url.str(), path);
        info("http service '{}' export path '{}'", url.str(), genericPath);

        return () -> {
            skeletonMap.remove(path);
            skeletonMap.remove(genericPath);
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T doReference(Class<T> interfaceC, Url url) {
        boolean useGeneric = url.getBooleanParam(Constants.GENERIC_KEY);
        boolean byteCodeInvoke = url.getBooleanParam(Constants.BYTE_CODE_INVOKE_KEY);

        //构建json rpc proxy
        JsonProxyFactoryBean jsonProxyFactoryBean = new JsonProxyFactoryBean();
        //设置jackson
        jsonProxyFactoryBean.setObjectMapper(JSON.PARSER);
        HttpRpcProxyFactoryBean httpRpcProxyFactoryBean = new HttpRpcProxyFactoryBean(jsonProxyFactoryBean);
//        httpRpcProxyFactoryBean.setRemoteInvocationFactory((methodInvocation) -> {
//            RemoteInvocation invocation = new HttpRemoteInvocation(methodInvocation);
//            if (useGeneric) {
//                invocation.addAttribute(Constants.GENERIC_KEY, useGeneric);
//            }
//            return invocation;
//        });

        //json rpc请求的url path
        String key = url.identityStr();
        if (useGeneric) {
            key = key + "/" + Constants.GENERIC;
        }
        httpRpcProxyFactoryBean.setServiceUrl(key);

        info("http reference '{}' refer path '{}'", url.str(), key);

        //设置服务接口
        if (useGeneric) {
            httpRpcProxyFactoryBean.setServiceInterface(GenericRpcService.class);
        } else {
            httpRpcProxyFactoryBean.setServiceInterface(interfaceC);
        }
        jsonProxyFactoryBean.afterPropertiesSet();
        //获取代理类
        Object proxy = jsonProxyFactoryBean.getObject();
        if (useGeneric) {
            if (byteCodeInvoke) {
                return javassistProxyedGenericRpcService((GenericRpcService) proxy, interfaceC);
            } else {
                return reflectProxyedGenericRpcService((GenericRpcService) proxy, interfaceC);
            }
        } else {
            return (T) proxy;
        }
    }

    @Override
    public void destroy() {
        for (HttpServer httpServer : serverMap.values()) {
            httpServer.close();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * http server handler
     */
    private class InternalHandler implements HttpHandler {
        private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
        private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
        private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

        private final boolean cors;

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
