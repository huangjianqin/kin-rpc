package org.kin.kinrpc.service;

import org.kin.kinrpc.MetadataResponse;

/**
 * 内置应用服务元数据访问服务
 * // TODO: 2023/7/19 一般来说, 服务不会变更, 暂不提供监听服务变更接口
 *
 * @author huangjianqin
 * @date 2023/7/17
 */
public interface MetadataService {
    /**
     * 返回本应用所有服务元数据
     *
     * @return {@link MetadataResponse}实例
     */
    MetadataResponse metadata();
}