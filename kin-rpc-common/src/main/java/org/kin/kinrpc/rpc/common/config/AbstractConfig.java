package org.kin.kinrpc.config;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.common.constants.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.common.config1.ProtocolType;
import org.kin.kinrpc.rpc.common.config1.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by huangjianqin on 2019/6/25.
 */
abstract class AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(AbstractConfig.class);

    /**
     * 检查配置参数正确性
     *
     * @throws Exception 异常
     */
    abstract void check();

    /**
     * 根据配置创建唯一URL
     */
    protected Url createURL(ApplicationConfig applicationConfig,
                            String hostPort,
                            AbstractRegistryConfig registryConfig,
                            Map<String, String> otherParams,
                            ProtocolType protocolType) {
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(protocolType)) {
            sb.append(protocolType.name().toLowerCase() + "://");
        }
        sb.append(hostPort);
        //目前是/serviceName#version
        sb.append("/")
                .append(Url.serviceKey(
                        otherParams.get(Constants.GROUP_KEY),
                        otherParams.get(Constants.SERVICE_KEY),
                        otherParams.get(Constants.VERSION_KEY)));
        sb.append("?");

        Map<String, String> params = new HashMap<>(Constants.URL_PARAM_NUM);
        params.put(Constants.APP_KEY, applicationConfig.getApp());
        if (registryConfig != null) {
            if (registryConfig instanceof DirectURLsRegistryConfig) {
                params.put(Constants.REGISTRY_KEY, RegistryType.DIRECTURLS.getType());
            } else if (registryConfig instanceof ZookeeperRegistryConfig) {
                params.put(Constants.REGISTRY_KEY, RegistryType.ZOOKEEPER.getType());
            } else {
                throw new IllegalStateException("unknown registry");
            }
            params.put(Constants.REGISTRY_URL_KEY, registryConfig.getAddress());
            if (registryConfig instanceof ZookeeperRegistryConfig) {
                params.put(Constants.SESSION_TIMEOUT_KEY, ((ZookeeperRegistryConfig) registryConfig).getSessionTimeout() + "");
            }
        }

        params.putAll(otherParams);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        //去掉最后一个&
        sb.replace(sb.length() - 1, sb.length(), "");

        return Url.of(sb.toString());
    }
}
