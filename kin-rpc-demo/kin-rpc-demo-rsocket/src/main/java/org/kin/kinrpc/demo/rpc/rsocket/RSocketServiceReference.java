package org.kin.kinrpc.demo.rpc.rsocket;

import io.netty.buffer.Unpooled;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import reactor.core.publisher.Flux;

/**
 * @author huangjianqin
 * @date 2021/1/31
 */
public class RSocketServiceReference {
    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<RSocketService> config = References.reference(RSocketService.class)
                .version("001")
                .callTimeout(2000)
                .rate(10000);
        config.urls("rsocket://0.0.0.0:16888");
        RSocketService service = config.get();

        service.fireAndForget(SimpleRequest.newBuilder().setRequestMessage("hi").build(), Unpooled.EMPTY_BUFFER);
        SimpleResponse replyResponse = service.requestReply(SimpleRequest.newBuilder().setRequestMessage("i am god").build(), Unpooled.EMPTY_BUFFER).block();
        System.out.println(replyResponse.getResponseMessage());

        Flux<SimpleResponse> flux = service.requestStream(SimpleRequest.newBuilder().setRequestMessage("open stream").build(), Unpooled.EMPTY_BUFFER);
        flux.subscribe(System.out::println);

        Thread.sleep(20_000);

        System.out.println("结束");
        config.disable();
        System.exit(0);
    }
}
