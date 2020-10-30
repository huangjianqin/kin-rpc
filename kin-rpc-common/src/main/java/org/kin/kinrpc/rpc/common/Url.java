package org.kin.kinrpc.rpc.common;

import org.kin.framework.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Url implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Url.class);
    /** 协议号 */
    private String protocol;
    /** 用户名 */
    private String username;
    /** 密码 */
    private String password;
    /** 主机号 */
    private String host;
    /** 端口 */
    private int port;
    /** url */
    private String path;
    /** 参数 */
    private Map<String, String> params;
    /** 服务名 */
    private String serviceName;

    public Url(String protocol, String username, String password, String host, int port, String path, Map<String, String> params) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.params = params;

        this.serviceName = getParam(Constants.SERVICE_NAME_KEY);
        String version = getParam(Constants.VERSION_KEY);
        if(StringUtils.isNotBlank(version)){
            this.serviceName += ("#" + version);
        }
    }

    public static Url valueOf(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url == null");
        }
        String username = null;
        String password = null;
        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;
        // seperator between body and parameters
        int i = url.indexOf("?");
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("\\&");
            parameters = new HashMap<>();
            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        try {
                            parameters.put(part.substring(0, j), URLDecoder.decode(part.substring(j + 1), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            log.error("", e);
                        }
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            }
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                }
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }
        i = url.indexOf("@");
        if (i >= 0) {
            username = url.substring(0, i);
            int j = username.indexOf(":");
            if (j >= 0) {
                password = username.substring(j + 1);
                username = username.substring(0, j);
            }
            url = url.substring(i + 1);
        }
        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) {
            host = url;
        }
        return new Url(protocol, username, password, host, port, path, parameters);
    }

    public String getParam(String k) {
        return params.getOrDefault(k, "");
    }

    //getter

    public String getProtocol() {
        return protocol;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getServiceName() {
        return serviceName;
    }
}