package org.kin.kinrpc.constants;

/**
 * 服务元数据常量
 *
 * @author huangjianqin
 * @date 2023/6/27
 */
public final class ServiceMetadataConstants {
    //-----------------------------------------------------------------------------------------------元数据key
    /** 服务schema */
    public static final String SCHEMA_KEY = "schema";
    /** 服务权重 */
    public static final String WEIGHT_KEY = "weight";
    /** 服务序列化方式 */
    public static final String SERIALIZATION_KEY = "serialization";
    /** service token */
    public static final String TOKEN_KEY = "token";
    /** appName */
    public static final String APP_NAME_KEY = "appName";

    private ServiceMetadataConstants() {
    }
}
