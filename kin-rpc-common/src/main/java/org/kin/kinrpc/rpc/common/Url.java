package org.kin.kinrpc.rpc.common;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Url implements Serializable, Cloneable {
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
    /** serviceName-version */
    private String serviceKey;
    /** 接口名 */
    private String interfaceN;

    public Url(String protocol, String username, String password, String host, int port, String path, Map<String, String> params) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.params = params;

        this.serviceKey = serviceKey(getParam(Constants.GROUP_KEY), getParam(Constants.SERVICE_KEY), getParam(Constants.VERSION_KEY));
        this.interfaceN = getParam(Constants.INTERFACE_KEY);
    }

    /**
     * parse url string
     */
    public static Url of(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url == null");
        }
        String username = null;
        String password = null;
        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = Collections.emptyMap();
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
                            ExceptionUtils.throwExt(e);
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

    /**
     * 合并url, consumer > provider
     */
    public static Url mergeUrl(Url sinkUrl, Url sourceUrl) {
        Map<String, String> params = sourceUrl.params;
        if (CollectionUtils.isEmpty(params)) {
            params = new HashMap<>();
        }
        params.putAll(sinkUrl.params);
        return new Url(
                sourceUrl.protocol, sourceUrl.username, sourceUrl.password,
                sourceUrl.host, sourceUrl.port,
                StringUtils.isNotBlank(sourceUrl.path) ? sourceUrl.path : sinkUrl.path,
                params);
    }

    //----------------------------------------------------------------------------------------------------------------

    /**
     * @return 是否包含某个参数
     */
    public boolean containsParam(String k) {
        return params.containsKey(k);
    }

    /**
     * 获取url参数
     */
    public String getParam(String k) {
        return getParam(k, "");
    }

    /**
     * 获取url参数
     */
    public String getParam(String k, String defalutVal) {
        return params.getOrDefault(k, defalutVal);
    }

    /**
     * 获取url参数(数字类型, 默认返回0)
     */
    private String getNumberParam(String k) {
        String param = getParam(k);
        return StringUtils.isNotBlank(param) ? param : "0";
    }

    /**
     * 获取url参数(boolean类型)
     */
    public boolean getBooleanParam(String k) {
        return getBooleanParam(k, false);
    }

    /**
     * 获取url参数(boolean类型)
     */
    public boolean getBooleanParam(String k, boolean defalutVal) {
        String param = getParam(k);
        return StringUtils.isNotBlank(param) ? Boolean.parseBoolean(param) : defalutVal;
    }

    /**
     * 获取url参数(int类型, 默认返回0)
     */
    public int getIntParam(String k) {
        return Integer.parseInt(getNumberParam(k));
    }

    /**
     * 获取url参数(int类型, 默认返回defalutVal)
     */
    public int getIntParam(String k, int defalutVal) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defalutVal;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * 获取url参数(long类型, 默认返回0)
     */
    public long getLongParam(String k) {
        return Long.parseLong(getNumberParam(k));
    }

    /**
     * 获取url参数(long类型, 默认返回defalutVal)
     */
    public long getLongParam(String k, long defalutVal) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defalutVal;
        } else {
            return Long.parseLong(value);
        }
    }

    /**
     * 获取url参数(double类型, 默认返回0)
     */
    public double getDoubleParam(String k) {
        return Double.parseDouble(getNumberParam(k));
    }

    /**
     * 获取url参数(double类型, 默认返回defalutVal)
     */
    public double getDoubleParam(String k, double defalutVal) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defalutVal;
        } else {
            return Double.parseDouble(value);
        }
    }

    /**
     * url object to string
     */
    public String str() {
        return str(true);
    }

    /**
     * url object to string, 不包括参数
     */
    public String identityStr() {
        return str(false);
    }

    /**
     * url object to string
     *
     * @param appendParameter 是否添加参数
     */
    public String str(boolean appendParameter) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotBlank(protocol)) {
            buf.append(protocol);
            buf.append("://");
        }
        if (StringUtils.isNotBlank(username)) {
            buf.append(username);
            if (StringUtils.isNotBlank(password)) {
                buf.append(":");
                buf.append(password);
            }
            buf.append("@");
        }

        if (StringUtils.isNotBlank(host)) {
            buf.append(host);
            if (port > 0) {
                buf.append(":");
                buf.append(port);
            }
        }

        if (StringUtils.isNotBlank(path)) {
            buf.append("/");
            buf.append(path);
        }

        if (appendParameter && CollectionUtils.isNonEmpty(params)) {
            boolean first = true;
            for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
                if (StringUtils.isNotBlank(entry.getKey())) {
                    if (first) {
                        buf.append("?");
                        first = false;
                    } else {
                        buf.append("&");
                    }
                    buf.append(entry.getKey());
                    buf.append("=");
                    buf.append(entry.getValue() == null ? "" : entry.getValue().trim());
                }
            }
        }
        return buf.toString();
    }

    /**
     * @return 网络地址
     */
    public String getAddress() {
        return NetUtils.getIpPort(getHost(), getPort());
    }

    /**
     * 关联额外参数, 主要用于支持不同协议层的额外配置
     */
    public void attach(Map<String, Object> attachment) {
        for (Map.Entry<String, Object> entry : attachment.entrySet()) {
            params.put(entry.getKey(), entry.getValue().toString());
        }
    }

    @Override
    public Url clone() {
        return new Url(protocol, username, password, host, port, path, params);
    }

    @Override
    public String toString() {
        return str();
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

    public String getServiceKey() {
        return serviceKey;
    }

    public int getServiceId() {
        return serviceKey.hashCode();
    }

    public String getService() {
        return params.get(Constants.SERVICE_KEY);
    }

    public String getInterfaceN() {
        return interfaceN;
    }

    //------------------------------static------------------------------

    /**
     * @return 服务唯一标识
     */
    public static String serviceKey(String group, String service, String version) {
        return group + "#" + service + ":" + version;
    }

    /**
     * @return hash(服务唯一标识)
     */
    public static Integer serviceId(String group, String service, String version) {
        return serviceKey(group, service, version).hashCode();
    }
}