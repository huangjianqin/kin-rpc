package org.kin.kinrpc.common;

import org.kin.framework.utils.HttpUtils;

import java.util.Map;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class URL {
    private String protocol;
    private String host;
    private int port;
    private String serviceName;
    private Map<String, String> params;

    public URL(java.net.URL url){
        protocol = url.getProtocol();
        host = url.getHost();
        port = url.getPort();
        serviceName = url.getPath().replaceAll("/", ".");
        params = HttpUtils.parseQuery(url.getQuery());

    }

    public String getParam(String k){
        return params.get(k);
    }

    //getter
    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getServiceName() {
        return serviceName;
    }
}
