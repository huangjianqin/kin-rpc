package org.kin.kinrpc.demo.boot.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author huangjianqin
 * @date 2023/7/19
 */
@RestController("consumer")
public class ConsumerController {
    @Autowired
    private DemoServiceConsumer demoServiceConsumer;

    @PostMapping("/exec")
    public void exec() {
        demoServiceConsumer.run(null);
    }
}
