package org.kin.kinrpc.config;

import org.kin.framework.collection.AttachmentMap;
import org.kin.framework.collection.AttachmentSupport;
import org.kin.framework.utils.IllegalFormatException;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.YamlUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 继承自{@link AttachmentMap}用于用户自定义配置项
 * 当用户自定义registry或者protocol时,就可以通过{@link AttachmentMap}定义自定义配置项
 *
 * @author huangjianqin
 * @date 2023/6/15
 */
public abstract class AttachableConfig extends AbstractConfig implements AttachmentSupport {
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

    @Override
    public void attachMany(Map<String, ?> attachments) {
        this.attachments.putAll(attachments);
    }

    @Override
    public void attachMany(AttachmentMap other) {
        this.attachments.putAll(other.attachments());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T attach(String key, Object obj) {
        return (T) attachments.put(key, obj);
    }

    /**
     * 是否存在attachment key
     *
     * @param key attachment key
     * @return true表示存在attachment key
     */
    @Override
    public boolean hasAttachment(String key) {
        return attachments.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T attachment(String key) {
        return (T) attachments.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T attachment(String key, T defaultValue) {
        return (T) attachments.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean boolAttachment(String key, boolean defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Boolean.class.equals(valueClass) ||
                    Boolean.TYPE.equals(valueClass)) {
                return (boolean) value;
            } else if (String.class.equals(valueClass)) {
                String valueStr = (String) value;
                if (StringUtils.isNumeric(valueStr)) {
                    return Long.parseLong(valueStr) > 0;
                } else {
                    return Boolean.parseBoolean(value.toString());
                }
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a boolean", value));
            }
        }
        return defaultValue;
    }

    @Override
    public byte byteAttachment(String key, byte defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Byte.class.equals(valueClass) ||
                    Byte.TYPE.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Byte.parseByte(value.toString());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a byte", value));
            }
        }
        return defaultValue;
    }

    @Override
    public short shortAttachment(String key, short defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Short.class.equals(valueClass) ||
                    Short.TYPE.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Short.parseShort(value.toString());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a short", value));
            }
        }
        return defaultValue;
    }

    @Override
    public int intAttachment(String key, int defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Integer.class.equals(valueClass) ||
                    Integer.TYPE.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Integer.parseInt(value.toString());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a integer", value));
            }
        }
        return defaultValue;
    }

    @Override
    public long longAttachment(String key, long defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Long.class.equals(valueClass) ||
                    Long.TYPE.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Long.parseLong(value.toString());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a long", value));
            }
        }
        return defaultValue;
    }

    @Override
    public float floatAttachment(String key, float defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Float.class.equals(valueClass) ||
                    Float.TYPE.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Float.parseFloat(value.toString());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a float", value));
            }
        }
        return defaultValue;
    }

    @Override
    public double doubleAttachment(String key, double defaultValue) {
        Object value = attachment(key);
        if (Objects.nonNull(value)) {
            Class<?> valueClass = value.getClass();
            if (Double.class.equals(valueClass) ||
                    Double.TYPE.equals(valueClass)) {
                return (byte) value;
            } else if (String.class.equals(valueClass)) {
                return Double.parseDouble(value.toString());
            } else {
                throw new IllegalFormatException(String.format("attachment '%s' is not a double", value));
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T detach(String key) {
        return (T) attachments.remove(key);
    }

    @Override
    public Map<String, Object> attachments() {
        return attachments;
    }

    @Override
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
        AttachableConfig that = (AttachableConfig) o;
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
