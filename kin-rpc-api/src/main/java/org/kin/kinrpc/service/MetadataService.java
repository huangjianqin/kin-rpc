package org.kin.kinrpc.service;

import org.kin.kinrpc.MetadataResponse;

/**
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
