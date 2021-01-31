package org.kin.kinrpc.demo.rpc.rsocket;

import com.google.protobuf.Empty;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2021/1/30
 */
public class RSocketServiceImpl implements RSocketService {
    @Override
    public Mono<Empty> fireAndForget(SimpleRequest message, ByteBuf metadata) {
        System.out.println("got message -> " + message.getRequestMessage());
        return Mono.just(Empty.getDefaultInstance());
    }

    @Override
    public Mono<SimpleResponse> requestReply(SimpleRequest message, ByteBuf metadata) {
        return Mono.fromCallable(
                () ->
                        SimpleResponse.newBuilder()
                                .setResponseMessage("we got the message -> " + message.getRequestMessage())
                                .build());
    }

    @Override
    public Mono<SimpleResponse> streamingRequestSingleResponse(Publisher<SimpleRequest> messages, ByteBuf metadata) {
        return Flux.from(messages)
                .windowTimeout(10, Duration.ofSeconds(500))
                .take(1)
                .flatMap(Function.identity())
                .reduce(
                        new ConcurrentHashMap<Character, AtomicInteger>(),
                        (map, s) -> {
                            char[] chars = s.getRequestMessage().toCharArray();
                            for (char c : chars) {
                                map.computeIfAbsent(c, _c -> new AtomicInteger()).incrementAndGet();
                            }

                            return map;
                        })
                .map(
                        map -> {
                            StringBuilder builder = new StringBuilder();

                            map.forEach(
                                    (character, atomicInteger) -> {
                                        builder
                                                .append("character -> ")
                                                .append(character)
                                                .append(", count -> ")
                                                .append(atomicInteger.get())
                                                .append("\n");
                                    });

                            String s = builder.toString();

                            return SimpleResponse.newBuilder().setResponseMessage(s).build();
                        });
    }

    @Override
    public Flux<SimpleResponse> requestStream(SimpleRequest message, ByteBuf metadata) {
        String requestMessage = message.getRequestMessage();
        return Flux.interval(Duration.ofMillis(200))
                .onBackpressureDrop()
                .map(i -> i + " - got message - " + requestMessage)
                .map(s -> SimpleResponse.newBuilder().setResponseMessage(s).build());
    }

    @Override
    public Flux<SimpleResponse> streamingRequestAndResponse(Publisher<SimpleRequest> messages, ByteBuf metadata) {
        return Flux.from(messages).flatMap(e -> requestReply(e, metadata));
    }
}
