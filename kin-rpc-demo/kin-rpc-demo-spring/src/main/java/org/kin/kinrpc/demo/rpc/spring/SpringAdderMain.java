package org.kin.kinrpc.demo.rpc.spring;

import org.kin.kinrpc.boot.EnableKinRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
@SpringBootApplication
@EnableKinRpc(scanBasePackages = "org.kin.kinrpc.demo")
public class SpringAdderMain {
    public static void main(String[] args) {
        SpringApplication.run(SpringAdderMain.class);
    }
}
