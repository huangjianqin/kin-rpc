package org.kin.kinrpc.demo.rpc.spring;

import org.kin.kinrpc.spring.EnableKinRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
@SpringBootApplication
@EnableKinRpc
public class SpringAdderMain {
    public static void main(String[] args) {
        SpringApplication.run(SpringAdderMain.class);
    }
}
