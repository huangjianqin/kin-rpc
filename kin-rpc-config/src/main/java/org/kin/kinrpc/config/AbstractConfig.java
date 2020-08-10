package org.kin.kinrpc.config;

import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangjianqin on 2019/6/25.
 */
abstract class AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(AbstractConfig.class);
    /**
     * 检查配置参数正确性
     * @throws Exception 异常
     */
    abstract void check() throws Exception;

    protected Url createURL(ApplicationConfig applicationConfig,
                            String hostPort,
                            AbstractRegistryConfig registryConfig,
                            Map<String, String> otherParams) {
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.KINRPC_PROTOCOL + "://").append(hostPort).append("?");

        Map<String, String> params = new HashMap<>(Constants.URL_PARAM_NUM);
        params.put(Constants.APP_NAME_KEY, applicationConfig.getAppName());
        if (registryConfig != null) {
            if (registryConfig instanceof DirectURLsRegistryConfig) {
                params.put(Constants.REGISTRY_KEY, RegistryType.DIRECTURLS.getType());
            } else if (registryConfig instanceof ZookeeperRegistryConfig) {
                params.put(Constants.REGISTRY_KEY, RegistryType.ZOOKEEPER.getType());
            } else if (registryConfig instanceof Zookeeper2RegistryConfig) {
                params.put(Constants.REGISTRY_KEY, RegistryType.ZOOKEEPER2.getType());
            } else {
                throw new IllegalStateException("unknown registry");
            }
            params.put(Constants.REGISTRY_URL_KEY, registryConfig.getAddress());
            params.put(Constants.SESSION_TIMEOUT_KEY, registryConfig.getSessionTimeout() + "");
        }

        params.putAll(otherParams);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                log.error("", e);
            }
        }

        //去掉最后一个&
        sb.replace(sb.length() - 1, sb.length(), "");

        return Url.valueOf(sb.toString());
    }
}
