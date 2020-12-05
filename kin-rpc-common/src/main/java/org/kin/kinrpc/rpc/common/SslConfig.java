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

    /**
     *
     */
    private String serverKeyCertChainPath;
    /**
     *
     */
    private String serverPrivateKeyPath;
    /**
     *
     */
    private String serverKeyPassword;
    /**
     *
     */
    private String serverTrustCertCollectionPath;

    /**
     *
     */
    private String clientKeyCertChainPath;
    /**
     *
     */
    private String clientPrivateKeyPath;
    /**
     *
     */
    private String clientKeyPassword;
    /**
     *
     */
    private String clientTrustCertCollectionPath;

    /**
     *
     */
    private InputStream serverKeyCertChainPathStream;
    /**
     *
     */
    private InputStream serverPrivateKeyPathStream;
    /**
     *
     */
    private InputStream serverTrustCertCollectionPathStream;

    /**
     *
     */
    private InputStream clientKeyCertChainPathStream;
    /**
     *
     */
    private InputStream clientPrivateKeyPathStream;
    /**
     *
     */
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
