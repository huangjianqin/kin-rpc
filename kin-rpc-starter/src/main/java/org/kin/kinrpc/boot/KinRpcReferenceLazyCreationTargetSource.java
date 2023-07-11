package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.ReferenceConfig;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;

/**
 * spring aop代理
 * lazy create reference
 *
 * @author huangjianqin
 * @date 2023/7/11
 */
public class KinRpcReferenceLazyCreationTargetSource extends AbstractLazyCreationTargetSource {
    /** reference config */
    private final ReferenceConfig<?> referenceConfig;

    public KinRpcReferenceLazyCreationTargetSource(ReferenceConfig<?> referenceConfig) {
        this.referenceConfig = referenceConfig;
    }

    @Override
    protected Object createObject() throws Exception {
        return referenceConfig.get();
    }

    @Override
    public synchronized Class<?> getTargetClass() {
        return referenceConfig.getInterfaceClass();
    }
}