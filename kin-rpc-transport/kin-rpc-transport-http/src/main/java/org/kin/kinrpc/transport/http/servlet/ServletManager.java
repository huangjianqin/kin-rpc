package org.kin.kinrpc.transport.http.servlet;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author huangjianqin
 * @date 2020/11/16
 */
public class ServletManager {
    private static final ServletManager INSTANCE = new ServletManager();

    public static ServletManager getInstance() {
        return INSTANCE;
    }

    /** key -> port, value -> http server servlet context缓存 */
    private final Map<Integer, ServletContext> contextMap = new ConcurrentHashMap<Integer, ServletContext>();

    /**
     * 添加ServletContext
     */
    public void addServletContext(int port, ServletContext servletContext) {
        contextMap.put(port, servletContext);
    }

    /**
     * 移除ServletContext
     */
    public void removeServletContext(int port) {
        contextMap.remove(port);
    }

    /**
     * 根据http server 绑定端口获取ServletContext
     *
     * @param port 端口
     * @return ServletContext
     */
    public ServletContext getServletContext(int port) {
        return contextMap.get(port);
    }
}
