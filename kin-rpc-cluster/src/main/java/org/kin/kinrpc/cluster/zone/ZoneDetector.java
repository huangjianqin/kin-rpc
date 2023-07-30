package org.kin.kinrpc.cluster.zone;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Invocation;

/**
 * user自定义获取zone逻辑
 *
 * @author huangjianqin
 * @date 2023/7/30
 */
@SPI(alias = "zoneDetector")
public interface ZoneDetector {
    /**
     * 返回zone
     *
     * @param invocation rpc call info
     * @return zone
     */
    String getZone(Invocation invocation);

    /**
     * 返回是否强制使用zone, 如果该zone下没有reference, 则抛错
     *
     * @param invocation rpc call info
     * @param zone       rpc call zone
     * @return true表示强制使用zone
     */
    boolean isZoneForcing(Invocation invocation, String zone);
}
