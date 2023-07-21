package org.kin.kinrpc.registry.metadata.service;

import org.kin.kinrpc.MetadataResponse;
import org.kin.kinrpc.registry.ApplicationMetadata;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.service.MetadataService;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
public class MetadataServiceImpl implements MetadataService {
    /** 单例 */
    private static final MetadataServiceImpl INSTANCE = new MetadataServiceImpl();

    public static MetadataService instance() {
        return INSTANCE;
    }

    @Override
    public MetadataResponse metadata(String revision) {
        for (DiscoveryRegistry discoveryRegistry : RegistryHelper.getDiscoveryRegistries()) {
            ApplicationMetadata appMetadata = discoveryRegistry.getServiceMetadataMap(revision);
            if (Objects.isNull(appMetadata)) {
                continue;
            }

            return new MetadataResponse(appMetadata.getRegisteredServiceMetadataMap());
        }

        return null;
    }
}
