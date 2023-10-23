package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import zipkin2.Call;
import zipkin2.Callback;

/**
 * 基于spring webmvc http client的zipkin sender实现
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
class ZipkinRestTemplateSender extends HttpSender {
    /** zipkin endpoint */
    private final String endpoint;
    /** spring webmvc http client */
    private final RestTemplate restTemplate;

    ZipkinRestTemplateSender(String endpoint,
                             RestTemplate restTemplate) {
        this.endpoint = endpoint;
        this.restTemplate = restTemplate;
    }

    @Override
    public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
        return new RestTemplateHttpPostCall(this.endpoint, batchedEncodedSpans, this.restTemplate);
    }

    //------------------------------------------------------------------------------------------------------------------
    private static class RestTemplateHttpPostCall extends HttpPostCall {
        /** zipkin endpoint */
        private final String endpoint;
        /** http client */
        private final RestTemplate restTemplate;

        RestTemplateHttpPostCall(String endpoint,
                                 byte[] body,
                                 RestTemplate restTemplate) {
            super(body);
            this.endpoint = endpoint;
            this.restTemplate = restTemplate;
        }

        @Override
        public Call<Void> clone() {
            return new RestTemplateHttpPostCall(this.endpoint, getUncompressedBody(), this.restTemplate);
        }

        @Override
        protected Void doExecute() {
            HttpEntity<byte[]> request = new HttpEntity<>(getBody(), getDefaultHeaders());
            this.restTemplate.exchange(this.endpoint, HttpMethod.POST, request, Void.class);
            return null;
        }

        @Override
        protected void doEnqueue(Callback<Void> callback) {
            try {
                doExecute();
                callback.onSuccess(null);
            } catch (Exception ex) {
                callback.onError(ex);
            }
        }

    }
}
