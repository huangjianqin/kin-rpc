package org.kin.kinrpc.metrics.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.common.lang.NonNullApi;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * observation documentation
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public enum KinRpcObservationDocumentation implements ObservationDocumentation {
    /**
     * Server side Observation.
     */
    SERVER {
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultServerObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeyNames.values();
        }

    },

    /**
     * Client side Observation.
     */
    CLIENT {
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultClientObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeyNames.values();
        }

    };

    /**
     * 定义key
     */
    @NonNullApi
    enum LowCardinalityKeyNames implements KeyName {

        /**
         * rpc system identity name key
         */
        RPC_SYSTEM {
            @Override
            public String asString() {
                return "rpc.system";
            }
        },

        /**
         * service name key
         */
        RPC_SERVICE {
            @Override
            public String asString() {
                return "rpc.service";
            }
        },

        /**
         * service handler name key
         */
        RPC_METHOD {
            @Override
            public String asString() {
                return "rpc.method";
            }
        },

        /**
         * remote invoker server hostname key
         */
        NET_PEER_NAME {
            @Override
            public String asString() {
                return "net.peer.name";
            }
        },

        /**
         * remote invoker server port key
         */
        NET_PEER_PORT {
            @Override
            public String asString() {
                return "net.peer.port";
            }
        }
    }
}
