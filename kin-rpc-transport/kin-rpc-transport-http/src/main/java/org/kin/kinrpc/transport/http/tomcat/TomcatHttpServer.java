package org.kin.kinrpc.transport.http.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.kin.framework.log.LoggerOprs;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.http.AbstractHttpServer;
import org.kin.kinrpc.transport.http.HttpHandler;
import org.kin.kinrpc.transport.http.servlet.DispatcherServlet;
import org.kin.kinrpc.transport.http.servlet.ServletManager;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class TomcatHttpServer extends AbstractHttpServer implements LoggerOprs {
    private final Tomcat tomcat;

    public TomcatHttpServer(Url url, HttpHandler handler) {
        super(url, handler);

        DispatcherServlet.addHttpHandler(url.getPort(), handler);
        String baseDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        tomcat = new Tomcat();

        Connector connector = tomcat.getConnector();
        connector.setPort(url.getPort());
//        connector.setProperty("maxThreads", String.valueOf(url.getParameter(THREADS_KEY, DEFAULT_THREADS)));
//        connector.setProperty("maxConnections", String.valueOf(url.getParameter(ACCEPTS_KEY, -1)));
        connector.setProperty("URIEncoding", "UTF-8");
        connector.setProperty("connectionTimeout", "60000");
        connector.setProperty("maxKeepAliveRequests", "-1");

        tomcat.setBaseDir(baseDir);
        tomcat.setPort(url.getPort());

        Context context = tomcat.addContext("/", baseDir);
        Tomcat.addServlet(context, "dispatcher", new DispatcherServlet());
        context.addServletMappingDecoded("/*", "dispatcher");
        ServletManager.getInstance().addServletContext(url.getPort(), context.getServletContext());

        // tell tomcat to fail on startup failures.
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new IllegalStateException("Failed to start tomcat server at " + url.getAddress(), e);
        }
    }

    @Override
    public void close() {
        super.close();

        ServletManager.getInstance().removeServletContext(url.getPort());

        try {
            tomcat.stop();
        } catch (Exception e) {
            warn(e.getMessage(), e);
        }
    }
}
