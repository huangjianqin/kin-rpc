package org.kin.kinrpc.demo.rpc.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
//todo 解决不用开发者配置包扫描和不开启代理
@SpringBootApplication(scanBasePackages = "org.kin.kinrpc", proxyBeanMethods = false)
public class SpringAdderMain {
    public static void main(String[] args) {
        SpringApplication.run(SpringAdderMain.class);
    }
}
