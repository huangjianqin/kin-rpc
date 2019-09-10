package org.kin.kinrpc.registry;

import org.kin.kinrpc.common.URL;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public interface RegistryFactory {
    Registry getRegistry(URL url);

    void close(URL url);
}
