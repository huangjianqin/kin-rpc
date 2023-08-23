package org.kin.kinrpc.demo.boot.consumer;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.RpcCallProfiler;
import org.kin.kinrpc.demo.api.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

import static com.alibaba.csp.sentinel.slots.block.RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO;

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

    @GetMapping("/initFlowRule")
    public String initFlowRule(@RequestParam("resource") String resource) {
        FlowRule flowRule = new FlowRule(resource);
        flowRule.setCount(1);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        List<FlowRule> flowRules = new ArrayList<>();
        flowRules.add(flowRule);
        FlowRuleManager.loadRules(flowRules);
        return flowRules.toString();
    }

    @GetMapping("/initDegradeRule")
    public String initDegradeRule(@RequestParam("resource") String resource) {
        DegradeRule degradeRule = new DegradeRule(resource)
                .setCount(0.5)
                .setGrade(DEGRADE_GRADE_EXCEPTION_RATIO);
        List<DegradeRule> degradeRules = new ArrayList<>();
        degradeRules.add(degradeRule);
        degradeRule.setTimeWindow(1);
        DegradeRuleManager.loadRules(degradeRules);
        return degradeRules.toString();
    }

    @GetMapping("/findFast")
    public int findFast() throws Exception {
        int num = 10;
        CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            ForkJoinPool.commonPool()
                    .execute(() -> {
                        //sentinel资源名demo:find(java.lang.String,int)
                        try {
                            System.out.println(demoService.find("A", 1));
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        latch.await();
        return ThreadLocalRandom.current().nextInt(1_000);
    }

    @GetMapping("/logRpcCallProfiler")
    public String logRpcCallProfiler() {
        return RpcCallProfiler.log();
    }
}
