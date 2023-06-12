/**
 * @author huangjianqin
 * @date 2023/6/12
 */
package org.kin.kinrpc.transport;

/**
 * 命名逻辑:
 * transport                        -> remoting                                             -> request
 * kinrpc, grpc, rsocket...            process command, usually resolved request object        process request
 *
 * 受限于某些transport实现限制, 目前transport层无法基于socket进行全双工通信, 也就是说client只能发送request, 处理response, 而server只能接受并处理request, 返回response
 * 在日常使用rpc或者message场景里面, 往往都是对外暴露server的地址, 本质上也无法实现基于socket进行全双工通信
 * 而中心化的broker实现, 由broker负责管理所有channel, channel之间通信仅能通过broker, 那么其相对容易实现基于socket进行全双工通信
 */