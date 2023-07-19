package org.kin.kinrpc.registry;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public class RegistryDiscoveryException extends RuntimeException {
    private static final long serialVersionUID = 2543150224511241140L;

    public RegistryDiscoveryException(String message) {
        super(message);
    }

    public RegistryDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryDiscoveryException(Throwable cause) {
        super(cause);
    }
}
