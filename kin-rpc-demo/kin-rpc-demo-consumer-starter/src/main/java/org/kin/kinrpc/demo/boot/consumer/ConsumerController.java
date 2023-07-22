package org.kin.kinrpc.demo.boot.consumer;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.demo.api.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2023/7/19
 */
@RestController()
@RequestMapping("/consumer")
public class ConsumerController {
    @Autowired
    private DemoServiceConsumer demoServiceConsumer;
    @Autowired
    private DemoService demoService;

    @GetMapping("/exec")
    public String exec() {
        ForkJoinPool.commonPool()
                .execute(() -> demoServiceConsumer.run(null));
        return "ok-" + StringUtils.randomString(ThreadLocalRandom.current().nextInt(20));
    }

    @GetMapping("/random")
    public Integer random() {
        return demoService.delayRandom();
    }
}
