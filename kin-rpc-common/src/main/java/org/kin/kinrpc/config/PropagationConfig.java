package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class PropagationConfig extends AbstractConfig {
    public static final String B3 = "B3";

    public static final String W3C = "W3C";

    /**
     * Tracing context propagation type.
     */
    private String type = DefaultConfig.DEFAULT_PROPAGATION_TYPE;

    public static PropagationConfig b3() {
        return new PropagationConfig().type(B3);
    }

    public static PropagationConfig w3c() {
        return new PropagationConfig().type(W3C);
    }

    public PropagationConfig type(String type) {
        this.type = type;
        return this;
    }

    //setter && getter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "{" +
                "type='" + type + '\'' +
                "}";
    }
}
