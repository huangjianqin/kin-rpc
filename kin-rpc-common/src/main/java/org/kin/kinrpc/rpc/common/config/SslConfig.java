package org.kin.kinrpc.rpc.common.config;

import java.io.File;

/**
 * ssl相关配置
 *
 * @author huangjianqin
 * @date 2023/6/18
 */
public class SslConfig implements Config {
    //--------------------------------------------ssl配置 start
    //--------------------------------------------cert和ca都配置了, 标识开启双向认证
    //--------------------------------------------server ssl配置
    /**
     * certificate chain file
     * 证书链文件, 所谓链, 即custom certificate -> root certificate
     */
    private File certFile;
    /** private key file */
    private File certKeyFile;
    /** the password of the {@code keyFile}, or {@code null} if it's not password-protected */
    private String certKeyPassword;
    //--------------------------------------------client ssl配置
    /**
     * 自定义信任证书集合
     * 信任证书即私钥提交CA签名后的证书, 用于校验server端证书权限
     * null, 则表示使用系统默认
     * TLS握手时需要
     */
    private File caFile;
    /** 证书指纹 */
    private File fingerprintFile;
    //--------------------------------------------ssl配置 end

    public static SslConfig create() {
        return new SslConfig();
    }

    private SslConfig() {
    }

    //setter && getter
    public File getCertFile() {
        return certFile;
    }

    public SslConfig certFile(File certFile) {
        this.certFile = certFile;
        return this;
    }

    public File getCertKeyFile() {
        return certKeyFile;
    }

    public SslConfig certKeyFile(File certKeyFile) {
        this.certKeyFile = certKeyFile;
        return this;
    }

    public String getCertKeyPassword() {
        return certKeyPassword;
    }

    public SslConfig certKeyPassword(String certKeyPassword) {
        this.certKeyPassword = certKeyPassword;
        return this;
    }

    public File getCaFile() {
        return caFile;
    }

    public SslConfig caFile(File caFile) {
        this.caFile = caFile;
        return this;
    }

    public File getFingerprintFile() {
        return fingerprintFile;
    }

    public SslConfig fingerprintFile(File fingerprintFile) {
        this.fingerprintFile = fingerprintFile;
        return this;
    }
}
