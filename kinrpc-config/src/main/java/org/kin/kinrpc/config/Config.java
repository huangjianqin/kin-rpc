package org.kin.kinrpc.config;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangjianqin on 2019/6/25.
 */
abstract class AbstractConfig {
    /**
     * 检查配置参数正确性
     */
    public abstract void check();

    protected URL createServiceURL(ApplicationConfig applicationConfig,
                                   ServerConfig serverConfig,
                                   AbstractRegistryConfig registryConfig, String serviceName){
        return createURL(applicationConfig, "0.0.0.0:" + serverConfig.getPort(), registryConfig, serviceName, Collections.EMPTY_MAP);
    }

    protected URL createReferenceURL(ApplicationConfig applicationConfig,
                                     AbstractRegistryConfig registryConfig,
                                     String serviceName, Map<String, String> otherParams){
        return createURL(applicationConfig, "0.0.0.0:0", registryConfig, serviceName, otherParams);
    }

    private URL createURL(ApplicationConfig applicationConfig,
                          String hostPort,
                          AbstractRegistryConfig registryConfig,
                          String serviceName, Map<String, String> otherParams){
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.KINRPC_PROTOCOL + "://" + hostPort + "/" + serviceName + "?");

        Map<String, String> params = new HashMap<>(Constants.URL_PARAM_NUM);
        params.put(Constants.APP_NAME_KEY, applicationConfig.getAppName());
        if(registryConfig instanceof DefaultRegistryConfig){
            params.put(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY);
        }
        else if(registryConfig instanceof ZookeeperRegistryConfig){
            params.put(Constants.REGISTRY_KEY, Constants.ZOOKEEPER_REGISTRY);
        }
        else{
            throw new IllegalStateException("unknown registry");
        }
        params.put(Constants.REGISTRY_URL_KEY, registryConfig.getUrl());
        params.put(Constants.REGISTRY_PASSWORD_KEY, registryConfig.getPassword());
        params.put(Constants.SESSION_TIMEOUT_KEY, registryConfig.getSessionTimeout() + "");

        params.putAll(otherParams);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                sb.append(entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8") + "&");
            } catch (UnsupportedEncodingException e) {
                ExceptionUtils.log(e);
            }
        }

        //去掉最后一个&
        sb.replace(sb.length() - 1, sb.length(), "");

        try {
            return new URL(new java.net.URL(sb.toString()));
        } catch (MalformedURLException e) {
            ExceptionUtils.log(e);
        }

        return null;
    }
}
