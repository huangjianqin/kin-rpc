package org.kin.kinrpc.config;

import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.IllegalFormatException;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.YamlUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * 继承自{@link AttachmentMap}用于用户自定义配置项
 * 当用户自定义registry或者protocol时,就可以通过{@link AttachmentMap}定义自定义配置项
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public abstract class AttachableConfig<AC extends AttachableConfig<AC>> extends AbstractConfig {
    /** attachments */
    private Map<String, Object> attachments = new HashMap<>();

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();

        //转换成property map
        Properties properties = YamlUtils.transfer2Properties(attachments);
        Map<String, Object> attachments = new HashMap<>(properties.size());
        for (Object key : properties.keySet()) {
            attachments.put(key.toString(), properties.get(key));
        }
        this.attachments = attachments;
    }

    /**
     * 强制转换成this
     *
     * @return this
     */
    @SuppressWarnings("unchecked")
    protected final AC castThis() {
        return (AC) this;
    }

    /**
     * 附带attachment
     *
     * @param attachments attachments
     * @return this
     */
    public AC attachMany(Map<String, ?> attachments) {
        this.attachments.putAll(attachments);
        return castThis();
    }

    /**
     * 附带attachment
     *
     * @param attachments attachments
     * @return this
     */
    public AC attachMany(AttachmentMap attachments) {
        return attachMany(attachments.attachments());
    }

    /**
     * 附带attachment
     *
     * @param key attachment key
     * @param obj attachment value
     * @return this
     */
    public AC attach(String key, Object obj) {
        attachments.put(key, obj);
        return castThis();
    }

    /**
     * 是否存在attachment key
     *
     * @param key attachment key
     * @return true表示存在attachment key
     */
    public boolean hasAttachment(String key) {
        return attachments.containsKey(key);
    }

    /**
     * 返回attachment value
     *
     * @param key attachment key
     * @return attachment value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T attachment(String key) {
        return (T) attachments.get(key);
    }

    /**
     * 返回attachment value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment value
     * @return attachment value
     */
    @SuppressWarnings("unchecked")
    public <T> T attachment(String key, T defaultValue) {
        return (T) attachments.getOrDefault(key, defaultValue);
    }

    /**
     * 返回attachment boolean value
     *
     * @param key attachment key
     * @return attachment boolean value
     */
    public boolean boolAttachment(String key) {
        return boolAttachment(key, false);
    }

    /**
     * 返回attachment boolean value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment boolean value
     * @return attachment boolean value
     */
    public boolean boolAttachment(String key, boolean defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Boolean.class.equals(valueClass)) {
                return (boolean) value;
            } else if (String.class.equals(valueClass)) {
                String valueStr = (String) value;
                if (StringUtils.isNumeric(valueStr)) {
                    return Long.parseLong(valueStr) > 0;
                } else {
                    return Boolean.parseBoolean(value.toString().trim());
                }
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a boolean", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment byte value
     *
     * @param key attachment key
     * @return attachment byte value
     */
    public byte byteAttachment(String key) {
        return byteAttachment(key, (byte) 0);
    }

    /**
     * 返回attachment byte value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment byte value
     * @return attachment byte value
     */
    public byte byteAttachment(String key, byte defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Byte.class.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Byte.parseByte(value.toString().trim());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a byte", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment short value
     *
     * @param key attachment key
     * @return attachment short value
     */
    public short shortAttachment(String key) {
        return shortAttachment(key, (short) 0);
    }

    /**
     * 返回attachment short value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment short value
     * @return attachment short value
     */
    public short shortAttachment(String key, short defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Short.class.equals(valueClass)) {
                return (short) value;
            } else if (String.class.equals(valueClass)) {
                return Short.parseShort(value.toString().trim());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a short", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment int value
     *
     * @param key attachment key
     * @return attachment int value
     */
    public int intAttachment(String key) {
        return intAttachment(key, 0);
    }

    /**
     * 返回attachment int value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment int value
     * @return attachment int value
     */
    public int intAttachment(String key, int defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Integer.class.equals(valueClass)) {
                return (int) value;
            } else if (String.class.equals(valueClass)) {
                return Integer.parseInt(value.toString().trim());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a integer", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment long value
     *
     * @param key attachment key
     * @return attachment long value
     */
    public long longAttachment(String key) {
        return longAttachment(key, 0L);
    }

    /**
     * 返回attachment long value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment long value
     * @return attachment long value
     */
    public long longAttachment(String key, long defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Long.class.equals(valueClass)) {
                return (long) value;
            } else if (String.class.equals(valueClass)) {
                return Long.parseLong(value.toString().trim());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a long", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment float value
     *
     * @param key attachment key
     * @return attachment float value
     */
    public float floatAttachment(String key) {
        return floatAttachment(key, 0F);
    }

    /**
     * 返回attachment float value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment float value
     * @return attachment float value
     */
    public float floatAttachment(String key, float defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Float.class.equals(valueClass)) {
                return (float) value;
            } else if (String.class.equals(valueClass)) {
                return Float.parseFloat(value.toString().trim());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a float", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment double value
     *
     * @param key attachment key
     * @return attachment double value
     */
    public double doubleAttachment(String key) {
        return doubleAttachment(key, 0D);
    }

    /**
     * 返回attachment double value, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param defaultValue 默认attachment double value
     * @return attachment double value
     */
    public double doubleAttachment(String key, double defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Double.class.equals(valueClass)) {
                return (double) value;
            } else if (String.class.equals(valueClass)) {
                return Double.parseDouble(value.toString().trim());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a double", value));
            }
        }
        return defaultValue;
    }

    /**
     * 返回attachment value, 并使用{@code func}进行值转换
     *
     * @param key  attachment key
     * @param func attachment convert function
     * @return attachment value
     */
    @Nullable
    public <T> T attachment(String key, Function<Object, T> func) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            return func.apply(value);
        }
        return null;
    }

    /**
     * 返回attachment value, 并使用{@code func}进行值转换, 如果不存在则取{@code defaultValue}
     *
     * @param key          attachment key
     * @param func         attachment convert function
     * @param defaultValue 默认attachment value
     * @return attachment value
     */
    public <T> T attachment(String key, Function<Object, T> func, T defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            return func.apply(value);
        }
        return defaultValue;
    }

    /**
     * 移除attachment
     *
     * @param key attachment key
     * @return attachment value if exists
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T detach(String key) {
        return (T) attachments.remove(key);
    }

    /**
     * 返回所有attachment
     *
     * @return 所有attachment
     */
    public Map<String, Object> attachments() {
        return attachments;
    }

    /**
     * 返回attachment map是否为空
     *
     * @return true表示attachment map为空
     */
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(attachments());
    }

    /**
     * 移除所有attachment
     */
    public void clear() {
        attachments.clear();
    }

    //setter && getter
    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttachableConfig)) {
            return false;
        }
        AttachableConfig<?> that = (AttachableConfig<?>) o;
        return Objects.equals(attachments, that.attachments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachments);
    }

    @Override
    public String toString() {
        return "attachments=" + attachments;
    }
}
