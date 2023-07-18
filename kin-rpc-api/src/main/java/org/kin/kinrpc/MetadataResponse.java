package org.kin.kinrpc;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/17
 */
public class MetadataResponse implements Serializable {
    private static final long serialVersionUID = -5790402423432923682L;

    /** key -> 服务唯一标识, value -> 服务元数据 */
    private Map<String, ServiceMetadata> serviceMetadataMap;

    public MetadataResponse() {
    }

    public MetadataResponse(Map<String, ServiceMetadata> serviceMetadataMap) {
        this.serviceMetadataMap = Collections.unmodifiableMap(serviceMetadataMap);
    }

    //setter && getter
    public Map<String, ServiceMetadata> getServiceMetadataMap() {
        return serviceMetadataMap;
    }

    public void setServiceMetadataMap(Map<String, ServiceMetadata> serviceMetadataMap) {
        this.serviceMetadataMap = serviceMetadataMap;
    }
}
