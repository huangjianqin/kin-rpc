package org.kin.kinrpc.transport.http.servlet;

import org.kin.kinrpc.transport.http.HttpHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = -5379914862946805751L;
    /** 单例 */
    private static DispatcherServlet INSTANCE;
    /** key -> port, value -> http handler */
    private static final Map<Integer, HttpHandler> HANDLERS = new ConcurrentHashMap<Integer, HttpHandler>();

    public static DispatcherServlet getInstance() {
        return INSTANCE;
    }

    public DispatcherServlet() {
        DispatcherServlet.INSTANCE = this;
    }

    /**
     * 添加 http handler
     */
    public static void addHttpHandler(int port, HttpHandler processor) {
        HANDLERS.put(port, processor);
    }

    /**
     * 移除 http handler
     */
    public static void removeHttpHandler(int port) {
        HANDLERS.remove(port);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpHandler handler = HANDLERS.get(request.getLocalPort());
        if (handler == null) {// service not found.
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Service not found.");
        } else {
            handler.handle(request, response);
        }
    }
}
