package org.kin.kinrpc.common;

import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.constants.UrlParamConstants;
import org.kin.kinrpc.utils.GsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class Url extends AttachmentMap implements Serializable {
    private static final long serialVersionUID = -6466196897378391517L;
    private static final Logger log = LoggerFactory.getLogger(Url.class);

    /** 协议号 */
    private final String protocol;
    /** 用户名 */
    private final String username;
    /** 密码 */
    private final String password;
    /** host */
    private final String host;
    /** 端口 */
    private final int port;
    /** url */
    private final String path;
    /** query参数, 一般用于应用配置参数共享 */
    private final Map<String, String> params;
    /** 服务gsv */
    private final String serviceKey;
    /** 服务gsv id */
    private final int serviceId;
    /** 接口名 */
    private final String interfaceName;

    public Url(String protocol, String username, String password, String host, int port, String path, Map<String, String> params) {
        this(protocol, username, password, host, port, path, params, null);
    }

    public Url(Url url, @Nullable Map<String, Object> attachmentMap) {
        this(url.protocol, url.username, url.password, url.host, url.port, url.path, url.params, attachmentMap);
    }

    public Url(Url url, @Nullable Map<String, String> params, @Nullable Map<String, Object> attachmentMap) {
        this(url.protocol, url.username, url.password, url.host, url.port, url.path, params, attachmentMap);
    }

    private Url(String protocol, String username, String password, String host, int port, String path,
                @Nullable Map<String, String> params, @Nullable Map<String, Object> attachmentMap) {
        this.protocol = protocol;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        if (Objects.nonNull(params)) {
            this.params = new HashMap<>(params);
        } else {
            this.params = new HashMap<>();
        }

        this.serviceKey = GsvUtils.service(getGroup(), getService(), getVersion());
        this.serviceId = GsvUtils.serviceId(serviceKey);
        this.interfaceName = getParam(UrlParamConstants.INTERFACE);
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
        // separator between body and parameters
        int i = url.indexOf("?");
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("&");
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
     * 基于其他{@link Url}和新的{@code attachmentMap}创建新的{@link Url}
     */
    public static Url newUrl(Url url, @Nullable Map<String, Object> attachmentMap) {
        return new Url(url, url.params, attachmentMap);
    }

    /**
     * 基于其他{@link Url},附加新的参数{@code params}和新的{@code attachmentMap}创建新的{@link Url}
     */
    public static Url newUrl(Url url, @Nullable Map<String, String> params, @Nullable Map<String, Object> attachmentMap) {
        return new Url(url, params, attachmentMap);
    }

    /**
     * encode url query param
     *
     * @param value url query param value
     * @return encoded  param value
     */
    public static String encode(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ExceptionUtils.throwExt(e);
            return null;
        }
    }

    /**
     * decode url query param
     *
     * @param value encoded  param value
     * @return encoded  decoded param value
     */
    public static String decode(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ExceptionUtils.throwExt(e);
            return null;
        }
    }

    //----------------------------------------------------------------------------------------------------------------

    /**
     * @return 是否包含指定query参数
     */
    public boolean containsParam(String k) {
        return params.containsKey(k);
    }

    /**
     * 获取query参数
     */
    public String getParam(String k) {
        return getParam(k, "");
    }

    /**
     * 获取query参数, 如果不存在, 则取{@code defaultValue}
     */
    public String getParam(String k, String defaultValue) {
        return params.getOrDefault(k, defaultValue);
    }

    /**
     * 获取query参数(数字类型, 默认返回0)
     */
    private String getNumberParam(String k) {
        String param = getParam(k);
        return StringUtils.isNotBlank(param) ? param : "0";
    }

    /**
     * 获取query参数(boolean类型)
     */
    public boolean getBooleanParam(String k) {
        return getBooleanParam(k, false);
    }

    /**
     * 获取query参数(boolean类型), 如果不存在, 则取{@code defaultValue}
     */
    public boolean getBooleanParam(String k, boolean defaultValue) {
        String param = getParam(k);
        return StringUtils.isNotBlank(param) ? Boolean.parseBoolean(param) : defaultValue;
    }

    /**
     * 获取query参数(short类型, 默认返回0)
     */
    public short getShortParam(String k) {
        return Short.parseShort(getNumberParam(k));
    }

    /**
     * 获取query参数(short类型), 如果不存在, 则取{@code defaultValue}
     */
    public short getShortParam(String k, short defaultValue) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        } else {
            return Short.parseShort(value);
        }
    }

    /**
     * 获取query参数(int类型, 默认返回0)
     */
    public int getIntParam(String k) {
        return Integer.parseInt(getNumberParam(k));
    }

    /**
     * 获取query参数(int类型), 如果不存在, 则取{@code defaultValue}
     */
    public int getIntParam(String k, int defaultValue) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * 获取query参数(long类型, 默认返回0)
     */
    public long getLongParam(String k) {
        return Long.parseLong(getNumberParam(k));
    }

    /**
     * 获取query参数(long类型), 如果不存在, 则取{@code defaultValue}
     */
    public long getLongParam(String k, long defaultValue) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        } else {
            return Long.parseLong(value);
        }
    }

    /**
     * 获取query参数(double类型, 默认返回0)
     */
    public double getDoubleParam(String k) {
        return Double.parseDouble(getNumberParam(k));
    }

    /**
     * 获取url参数(double类型), 如果不存在, 则取{@code defaultValue}
     */
    public double getDoubleParam(String k, double defaultValue) {
        String value = getParam(k);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        } else {
            return Double.parseDouble(value);
        }
    }

    /**
     * 获取query参数, 最后进行decode
     */
    public String getDecodedParam(String k) {
        return decode(getParam(k));
    }

    /**
     * 获取query参数, 如果不存在, 则取{@code defaultValue}, 最后进行decode
     */
    public String getDecodedParam(String k, String defaultValue) {
        return decode(getParam(k, defaultValue));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value param value
     * @return this
     */
    public Url putParam(String key, String value) {
        params.put(key, value);
        return this;
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value url encoded value
     * @return this
     */
    public Url encodeAndPutParam(String key, String value) {
        return putParam(key, encode(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value boolean value
     * @return this
     */
    public Url putParam(String key, boolean value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value byte value
     * @return this
     */
    public Url putParam(String key, byte value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value char value
     * @return this
     */
    public Url putParam(String key, char value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value short value
     * @return this
     */
    public Url putParam(String key, short value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value int value
     * @return this
     */
    public Url putParam(String key, int value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value float value
     * @return this
     */
    public Url putParam(String key, float value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value long value
     * @return this
     */
    public Url putParam(String key, long value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value double value
     * @return this
     */
    public Url putParam(String key, double value) {
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value {@link Number} value
     * @return this
     */
    public Url putParam(String key, Number value) {
        if (value == null) {
            return this;
        }
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value {@link CharSequence} value
     * @return this
     */
    public Url putParam(String key, CharSequence value) {
        if (value == null || value.length() == 0) {
            return this;
        }
        return putParam(key, String.valueOf(value));
    }

    /**
     * 添加query param
     *
     * @param key   param key
     * @param value {@link Enum} value
     * @return this
     */
    public Url putParam(String key, Enum<?> value) {
        if (value == null) {
            return this;
        }
        return putParam(key, String.valueOf(value));
    }

    /**
     * 批量添加query param
     *
     * @param params param map
     * @return this
     */
    public Url putParams(Map<String, String> params) {
        if (CollectionUtils.isEmpty(params)) {
            return this;
        }
        this.params.putAll(params);
        return this;
    }

    //----------------------------------------------------------------------------------------------------------------

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

    @Override
    public String toString() {
        return str();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Url)) {
            return false;
        }
        Url url = (Url) o;
        return port == url.port && Objects.equals(protocol, url.protocol) &&
                Objects.equals(username, url.username) &&
                Objects.equals(password, url.password) &&
                Objects.equals(host, url.host) &&
                Objects.equals(path, url.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, username, password, host, port, path);
    }

    public String getAddress() {
        return NetUtils.getIpPort(getHost(), getPort());
    }

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
        return serviceId;
    }

    public String getGroup() {
        return getParam(UrlParamConstants.GROUP);
    }

    public String getService() {
        return getParam(UrlParamConstants.SERVICE);
    }

    public String getVersion() {
        return getParam(UrlParamConstants.VERSION);
    }

    public String getInterfaceName() {
        return interfaceName;
    }
}