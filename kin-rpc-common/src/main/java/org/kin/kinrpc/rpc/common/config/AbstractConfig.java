package org.kin.kinrpc.rpc.common.config;

import org.kin.framework.collection.AttachmentMap;

/**
 * config抽象父类, 继承自{@link AttachmentMap}用于用户自定义配置项
 * 当用户自定义registry或者protocol时,就可以通过{@link AttachmentMap}定义自定义配置项
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public abstract class AbstractConfig extends AttachmentMap implements Config {
}
