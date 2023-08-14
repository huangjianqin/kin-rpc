package org.kin.kinrpc.registry.kubernetes;

/**
 * @author huangjianqin
 * @date 2023/8/13
 */
public final class KubernetesConstants {
    //-----------------------------------------------------------------------------------------------client
    /** kubernetes client masterUrl */
    public static final String CLIENT_MASTER_URL_KEY = "client.masterUrl";
    /** kubernetes client apiVersion */
    public static final String CLIENT_API_VERSION_KEY = "client.apiVersion";
    /** kubernetes client namespace */
    public static final String CLIENT_NAMESPACE_KEY = "client.namespace";
    /** kubernetes client username */
    public static final String CLIENT_USERNAME_KEY = "client.username";
    /** kubernetes client password */
    public static final String CLIENT_PASSWORD_KEY = "client.password";
    /** kubernetes client oauthToken */
    public static final String CLIENT_OAUTH_TOKEN_KEY = "client.oauthToken";
    /** kubernetes client caCertFile */
    public static final String CLIENT_CA_CERT_FILE_KEY = "client.caCertFile";
    /** kubernetes client caCertData */
    public static final String CLIENT_CA_CERT_DATA_KEY = "client.caCertData";
    /** kubernetes client clientKeyFile */
    public static final String CLIENT_KEY_FILE_KEY = "client.clientKeyFile";
    /** kubernetes client clientKeyData */
    public static final String CLIENT_KEY_DATA_KEY = "client.clientKeyData";
    /** kubernetes client clientCertFile */
    public static final String CLIENT_CERT_FILE_KEY = "client.clientCertFile";
    /** kubernetes client clientCertData */
    public static final String CLIENT_CERT_DATA_KEY = "client.clientCertData";
    /** kubernetes client clientKeyAlgo */
    public static final String CLIENT_KEY_ALGO_KEY = "client.clientKeyAlgo";
    /** kubernetes client clientKeyPassphrase */
    public static final String CLIENT_KEY_PASSPHRASE_KEY = "client.clientKeyPassphrase";
    /** kubernetes client connectionTimeout */
    public static final String CLIENT_CONNECTION_TIMEOUT_KEY = "client.connectionTimeout";
    /** kubernetes client requestTimeout */
    public static final String CLIENT_REQUEST_TIMEOUT_KEY = "client.requestTimeout";
    /** kubernetes client trustCerts */
    public static final String CLIENT_TRUST_CERTS_KEY = "client.trustCerts";
    /** kubernetes client httpProxy */
    public static final String CLIENT_HTTP_PROXY_KEY = "client.httpProxy";
    /** kubernetes client httpsProxy */
    public static final String CLIENT_HTTPS_PROXY_KEY = "client.httpsProxy";
    /** kubernetes client proxyUsername */
    public static final String CLIENT_PROXY_USERNAME_KEY = "client.proxyUsername";
    /** kubernetes client proxyPassword */
    public static final String CLIENT_PROXY_PASSWORD_KEY = "client.proxyPassword";
    /** kubernetes client noProxy */
    public static final String CLIENT_NO_PROXY_KEY = "client.noProxy";
    //-----------------------------------------------------------------------------------------------discovery
    /** kubernetes discovery namespace */
    public static final String DISCOVERY_NAMESPACE_KEY = "discovery.namespace";

    //-----------------------------------------------------------------------------------------------discovery default
    /** 默认kubernetes discovery namespace */
    public static final String DEFAULT_DISCOVERY_NAMESPACE = "default";

    private KubernetesConstants() {
    }
}
