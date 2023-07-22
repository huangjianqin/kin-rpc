package org.kin.kinrpc.service;

import org.kin.kinrpc.MetadataResponse;

/**
 * 内置应用服务元数据访问服务
 *
 * @author huangjianqin
 * @date 2023/7/17
 */
public interface MetadataService {
    /**
     * 返回本应用所有服务元数据
     *
     * @param revision 服务元数据版本
     * @return {@link MetadataResponse}实例
     */
    MetadataResponse metadata(String revision);
}
