package org.kin.kinrpc.boot.observability.autoconfigure.exporter.zipkin;

import org.springframework.http.HttpHeaders;
import org.springframework.util.unit.DataSize;
import zipkin2.Call;
import zipkin2.CheckResult;
import zipkin2.codec.Encoding;
import zipkin2.reporter.BytesMessageEncoder;
import zipkin2.reporter.ClosedSenderException;
import zipkin2.reporter.Sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * 使用HTTP client发送JSON spans
 * 支持自动gzip compression
 *
 * @author huangjianqin
 * @date 2023/8/26
 */
abstract class HttpSender extends Sender {
    /** http max size */
    private static final DataSize MESSAGE_MAX_SIZE = DataSize.ofKilobytes(512);

    /** closed */
    private volatile boolean closed;

    @Override
    public Encoding encoding() {
        return Encoding.JSON;
    }

    @Override
    public int messageMaxBytes() {
        return (int) MESSAGE_MAX_SIZE.toBytes();
    }

    @Override
    public int messageSizeInBytes(List<byte[]> encodedSpans) {
        return encoding().listSizeInBytes(encodedSpans);
    }

    @Override
    public int messageSizeInBytes(int encodedSizeInBytes) {
        return encoding().listSizeInBytes(encodedSizeInBytes);
    }

    @Override
    public CheckResult check() {
        try {
            sendSpans(Collections.emptyList()).execute();
            return CheckResult.OK;
        } catch (IOException | RuntimeException ex) {
            return CheckResult.failed(ex);
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    /**
     * 返回发送spans给zipkin endpoint的Http POST
     *
     * @param batchedEncodedSpans list of encoded spans as a byte array
     * @return zipkin {@link Call}
     */
    protected abstract HttpPostCall sendSpans(byte[] batchedEncodedSpans);

    @Override
    public Call<Void> sendSpans(List<byte[]> encodedSpans) {
        if (this.closed) {
            throw new ClosedSenderException();
        }
        return sendSpans(BytesMessageEncoder.JSON.encode(encodedSpans));
    }

    /**
     * 发送spans给zipkin endpoint的Http POST
     */
    abstract static class HttpPostCall extends Call.Base<Void> {
        /** >1kB就是用gzip压缩 */
        private static final DataSize COMPRESSION_THRESHOLD = DataSize.ofKilobytes(1);
        /** post body */
        private final byte[] body;

        HttpPostCall(byte[] body) {
            this.body = body;
        }

        /**
         * 返回http post body(可能是压缩后的)
         */
        protected byte[] getBody() {
            if (needsCompression()) {
                return compress(this.body);
            }
            return this.body;
        }

        /**
         * 返回raw http post body
         */
        protected byte[] getUncompressedBody() {
            return this.body;
        }

        /**
         * 返回http post default header
         */
        protected HttpHeaders getDefaultHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("b3", "0");
            headers.set("Content-Type", "application/json");
            if (needsCompression()) {
                headers.set("Content-Encoding", "gzip");
            }
            return headers;
        }

        /**
         * 返回http post body是否需要启动压缩
         */
        private boolean needsCompression() {
            return this.body.length > COMPRESSION_THRESHOLD.toBytes();
        }

        /**
         * 压缩http post body
         */
        private byte[] compress(byte[] input) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(result)) {
                gzip.write(input);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return result.toByteArray();
        }

    }
}
