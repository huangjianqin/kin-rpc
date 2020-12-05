package org.kin.kinrpc.rpc.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 全局ssl配置
 *
 * @author huangjianqin
 * @date 2020/12/5
 */
public class SslConfig {
    public static final SslConfig INSTANCE = new SslConfig();

    /** provider 证书路径 */
    private String serverKeyCertChainPath;
    /** provider 私钥路径 */
    private String serverPrivateKeyPath;
    /** provider 密钥密码路径 */
    private String serverKeyPassword;
    /** provider 信任证书路径 */
    private String serverTrustCertCollectionPath;

    /** reference 证书路径 */
    private String clientKeyCertChainPath;
    /** reference 私钥路径 */
    private String clientPrivateKeyPath;
    /** reference 密钥密码路径 */
    private String clientKeyPassword;
    /** reference 信任证书路径 */
    private String clientTrustCertCollectionPath;

    /** provider 证书路径stream */
    private InputStream serverKeyCertChainPathStream;
    /** provider 私钥路径stream */
    private InputStream serverPrivateKeyPathStream;
    /** provider 信任证书路径stream */
    private InputStream serverTrustCertCollectionPathStream;

    /** reference 证书路径 */
    private InputStream clientKeyCertChainPathStream;
    /** reference 私钥路径stream */
    private InputStream clientPrivateKeyPathStream;
    /** reference 信任证书路径stream */
    private InputStream clientTrustCertCollectionPathStream;

    private SslConfig() {
    }

    public InputStream getServerKeyCertChainPathStream() throws FileNotFoundException {
        if (serverKeyCertChainPath != null) {
            serverKeyCertChainPathStream = new FileInputStream(serverKeyCertChainPath);
        }
        return serverKeyCertChainPathStream;
    }

    public InputStream getServerPrivateKeyPathStream() throws FileNotFoundException {
        if (serverPrivateKeyPath != null) {
            serverPrivateKeyPathStream = new FileInputStream(serverPrivateKeyPath);
        }
        return serverPrivateKeyPathStream;
    }

    public InputStream getServerTrustCertCollectionPathStream() throws FileNotFoundException {
        if (serverTrustCertCollectionPath != null) {
            serverTrustCertCollectionPathStream = new FileInputStream(serverTrustCertCollectionPath);
        }
        return serverTrustCertCollectionPathStream;
    }

    public InputStream getClientKeyCertChainPathStream() throws FileNotFoundException {
        if (clientKeyCertChainPath != null) {
            clientKeyCertChainPathStream = new FileInputStream(clientKeyCertChainPath);
        }
        return clientKeyCertChainPathStream;
    }

    public InputStream getClientPrivateKeyPathStream() throws FileNotFoundException {
        if (clientPrivateKeyPath != null) {
            clientPrivateKeyPathStream = new FileInputStream(clientPrivateKeyPath);
        }
        return clientPrivateKeyPathStream;
    }

    public InputStream getClientTrustCertCollectionPathStream() throws FileNotFoundException {
        if (clientTrustCertCollectionPath != null) {
            clientTrustCertCollectionPathStream = new FileInputStream(clientTrustCertCollectionPath);
        }
        return clientTrustCertCollectionPathStream;
    }

    //setter && getter
    public String getServerKeyCertChainPath() {
        return serverKeyCertChainPath;
    }

    public void setServerKeyCertChainPath(String serverKeyCertChainPath) {
        this.serverKeyCertChainPath = serverKeyCertChainPath;
    }

    public String getServerPrivateKeyPath() {
        return serverPrivateKeyPath;
    }

    public void setServerPrivateKeyPath(String serverPrivateKeyPath) {
        this.serverPrivateKeyPath = serverPrivateKeyPath;
    }

    public String getServerKeyPassword() {
        return serverKeyPassword;
    }

    public void setServerKeyPassword(String serverKeyPassword) {
        this.serverKeyPassword = serverKeyPassword;
    }

    public String getServerTrustCertCollectionPath() {
        return serverTrustCertCollectionPath;
    }

    public void setServerTrustCertCollectionPath(String serverTrustCertCollectionPath) {
        this.serverTrustCertCollectionPath = serverTrustCertCollectionPath;
    }

    public String getClientKeyCertChainPath() {
        return clientKeyCertChainPath;
    }

    public void setClientKeyCertChainPath(String clientKeyCertChainPath) {
        this.clientKeyCertChainPath = clientKeyCertChainPath;
    }

    public String getClientPrivateKeyPath() {
        return clientPrivateKeyPath;
    }

    public void setClientPrivateKeyPath(String clientPrivateKeyPath) {
        this.clientPrivateKeyPath = clientPrivateKeyPath;
    }

    public String getClientKeyPassword() {
        return clientKeyPassword;
    }

    public void setClientKeyPassword(String clientKeyPassword) {
        this.clientKeyPassword = clientKeyPassword;
    }

    public String getClientTrustCertCollectionPath() {
        return clientTrustCertCollectionPath;
    }

    public void setClientTrustCertCollectionPath(String clientTrustCertCollectionPath) {
        this.clientTrustCertCollectionPath = clientTrustCertCollectionPath;
    }
}
