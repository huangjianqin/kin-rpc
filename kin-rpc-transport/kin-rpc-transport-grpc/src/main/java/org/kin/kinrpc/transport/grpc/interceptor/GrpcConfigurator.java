/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kin.kinrpc.transport.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.common.Url;

/**
 * 开发者自定义{@link NettyServerBuilder}, {@link NettyChannelBuilder},{@link CallOptions}
 */
@SPI(alias = "grpc.configurator")
public interface GrpcConfigurator {
    /**
     * grpc server 配置
     */
    default NettyServerBuilder configureServerBuilder(NettyServerBuilder builder, Url url) {
        return builder;
    }

    /**
     * grpc channel 配置
     */
    default NettyChannelBuilder configureChannelBuilder(NettyChannelBuilder builder, Url url) {
        return builder;
    }

    /**
     * server or channel额外配置
     */
    default CallOptions configureCallOptions(CallOptions options, Url url) {
        return options;
    }

}
