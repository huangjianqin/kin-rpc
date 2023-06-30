package org.kin.kinrpc.config;

import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.collection.AttachmentSupport;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 继承自{@link AttachmentMap}用于用户自定义配置项
 * 当用户自定义registry或者protocol时,就可以通过{@link AttachmentMap}定义自定义配置项
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public abstract class AttachableConfig extends AbstractConfig implements AttachmentSupport {
    /** attachments */
    private final AttachmentMap attachments = new AttachmentMap();

    @Override
    public final void attachMany(Map<String, ?> attachments) {
        this.attachments.attachMany(attachments);
    }

    @Override
    public final void attachMany(AttachmentMap other) {
        this.attachments.attachMany(other);
    }

    @Nullable
    @Override
    public final <T> T attach(String key, Object obj) {
        return this.attachments.attach(key, obj);
    }

    @Override
    public final boolean hasAttachment(String key) {
        return this.attachments.hasAttachment(key);
    }

    @Nullable
    @Override
    public final <T> T attachment(String key) {
        return this.attachments.attachment(key);
    }

    @Override
    public final <T> T attachment(String key, T defaultValue) {
        return this.attachments.attachment(key, defaultValue);
    }

    @Nullable
    @Override
    public final <T> T detach(String key) {
        return this.attachments.detach(key);
    }

    @Override
    public final Map<String, Object> attachments() {
        return this.attachments.attachments();
    }

    @Override
    public final void clear() {
        this.attachments.clear();
    }
}
