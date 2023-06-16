package org.kin.kinrpc.rpc.common;

import org.kin.framework.utils.CollectionUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/2/26
 */
public class AttributeHolder<AH extends AttributeHolder> {
    /** 属性Map */
    private Map<String, Object> attributes;

    public AttributeHolder() {
        this(Collections.emptyMap());
    }

    public AttributeHolder(Map<String, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    /**
     * 批量设置属性
     *
     * @param attributes 属性
     * @return this
     */
    @SuppressWarnings("unchecked")
    public AH putAttributes(Map<String, Object> attributes) {
        if (CollectionUtils.isNonEmpty(attributes)) {
            this.attributes.putAll(attributes);
        }
        return (AH) this;
    }

    /**
     * 设置属性
     *
     * @param key 属性key
     * @param obj 属性值
     * @return this
     */
    @SuppressWarnings("unchecked")
    public AH putAttribute(String key, Object obj) {
        if (obj == null) {
            attributes = new HashMap<>(4);
        }
        attributes.put(key, obj);
        return (AH) this;
    }

    /**
     * 是否包含指定属性
     *
     * @param key 属性key
     * @return 是否包含指定属性key
     */
    public boolean hasAttribute(String key) {
        return Objects.nonNull(attributes) && attributes.containsKey(key);
    }

    /**
     * 获取属性值
     *
     * @param key 属性key
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getAttribute(String key) {
        if (Objects.isNull(attributes)) {
            return null;
        }
        return (T) attributes.get(key);
    }

    /**
     * 获取属性值
     *
     * @param key          属性key
     * @param defaultValue 默认属性值
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getAttribute(String key, T defaultValue) {
        if (Objects.isNull(attributes)) {
            return defaultValue;
        }
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 移除属性
     *
     * @param key 属性key
     * @return this
     */
    @SuppressWarnings("unchecked")
    public AH removeAttribute(String key) {
        if (Objects.nonNull(attributes)) {
            attributes.remove(key);
        }
        return (AH) this;
    }

    /**
     * 设置属性
     *
     * @param key 属性key
     * @param obj 属性值
     * @return this
     */
    public AH putAttribute(AttributeKey key, Object obj) {
        return putAttribute(key.getName(), obj);
    }

    /**
     * 是否包含指定属性
     *
     * @param key 属性key
     * @return 是否包含指定属性key
     */
    public boolean hasAttribute(AttributeKey key) {
        return hasAttribute(key.getName());
    }

    /**
     * 获取属性值
     *
     * @param key 属性key
     * @return 属性值
     */
    public <T> T getAttribute(AttributeKey key) {
        return getAttribute(key.getName());
    }

    /**
     * 获取属性值
     *
     * @param key          属性key
     * @param defaultValue 默认属性值
     * @return 属性值
     */
    public <T> T getAttribute(AttributeKey key, T defaultValue) {
        return getAttribute(key.getName(), defaultValue);
    }

    /**
     * 移除属性
     *
     * @param key 属性key
     * @return this
     */
    public AH removeAttribute(AttributeKey key) {
        return removeAttribute(key.getName());
    }
}
