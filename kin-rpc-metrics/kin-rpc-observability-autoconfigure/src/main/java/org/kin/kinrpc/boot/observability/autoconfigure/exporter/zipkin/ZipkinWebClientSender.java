package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import zipkin2.Call;
import zipkin2.Callback;

/**
 * 基于spring webflux http client的zipkin sender实现
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
public class ZipkinWebClientSender extends HttpSender {
    /** zipkin endpoint */
    private final String endpoint;
    /** spring webflux http client */
    private final WebClient webClient;

    ZipkinWebClientSender(String endpoint, WebClient webClient) {
        this.endpoint = endpoint;
        this.webClient = webClient;
    }

    @Override
    public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
        return new WebClientHttpPostCall(this.endpoint, batchedEncodedSpans, this.webClient);
    }

    //------------------------------------------------------------------------------------------------------------------
    private static class WebClientHttpPostCall extends HttpPostCall {
        /** zipkin endpoint */
        private final String endpoint;
        /** spring webflux http client */
        private final WebClient webClient;

        WebClientHttpPostCall(String endpoint, byte[] body, WebClient webClient) {
            super(body);
            this.endpoint = endpoint;
            this.webClient = webClient;
        }

        @Override
        public Call<Void> clone() {
            return new WebClientHttpPostCall(this.endpoint, getUncompressedBody(), this.webClient);
        }

        @Override
        protected Void doExecute() {
            sendRequest().block();
            return null;
        }

        @Override
        protected void doEnqueue(Callback<Void> callback) {
            sendRequest().subscribe((entity) -> callback.onSuccess(null), callback::onError);
        }

        /**
         * 发送http request
         */
        private Mono<ResponseEntity<Void>> sendRequest() {
            return this.webClient.post()
                    .uri(this.endpoint)
                    .headers(this::addDefaultHeaders)
                    .bodyValue(getBody())
                    .retrieve()
                    .toBodilessEntity();
        }

        /**
         * 添加default header
         */
        private void addDefaultHeaders(HttpHeaders headers) {
            headers.addAll(getDefaultHeaders());
        }
    }
}