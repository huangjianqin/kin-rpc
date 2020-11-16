package org.kin.kinrpc.transport.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
@FunctionalInterface
public interface HttpHandler {
    /**
     * invoke request.
     *
     * @param request  request.
     * @param response response.
     */
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
}
