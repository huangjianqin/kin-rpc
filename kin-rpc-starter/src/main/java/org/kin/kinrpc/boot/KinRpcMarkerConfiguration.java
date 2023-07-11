package org.kin.kinrpc.boot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author huangjianqin
 * @date 2023/7/10
 */
@Configuration
public class KinRpcMarkerConfiguration {
    @Bean
    public Marker kinRpcMarkerBean() {
        return new Marker();
    }

    class Marker {

    }
}
