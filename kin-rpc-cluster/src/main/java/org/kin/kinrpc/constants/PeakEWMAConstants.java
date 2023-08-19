package org.kin.kinrpc.constants;

/**
 * @author huangjianqin
 * @date 2023/8/17
 */
public final class PeakEWMAConstants {
    /** peak EWMA数据的生命周期时间 */
    public static final String LIFE_TIME_KEY = "peakEwma.lifeTime";

    //-------------------------------------------------------------------------------------default
    /** 默认peak EWMA数据的生命周期时间 */
    public static final int DEFAULT_LIFE_TIME = 3_000;

    private PeakEWMAConstants() {
    }
}
