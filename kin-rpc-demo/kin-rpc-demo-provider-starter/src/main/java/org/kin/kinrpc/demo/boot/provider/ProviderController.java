package org.kin.kinrpc.demo.boot.provider;

import org.kin.kinrpc.RpcStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author huangjianqin
 * @date 2023/8/19
 */
@RestController()
@RequestMapping("/provider")
public class ProviderController {
    @GetMapping("/logRpcStatus")
    public String logRpcStatus() {
        return RpcStatus.log();
    }
}
