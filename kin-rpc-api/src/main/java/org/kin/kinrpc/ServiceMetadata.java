package org.kin.kinrpc;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/17
 */
public class ServiceMetadata implements Serializable {
    private static final long serialVersionUID = 2604592887097623563L;

    /** 服务元数据 */
    private Map<String, String> metadata;

    public ServiceMetadata() {
    }

    public ServiceMetadata(Map<String, String> metadata) {
        this.metadata = Collections.unmodifiableMap(metadata);
    }

    //setter && getter
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
