package org.kin.kinrpc.registry;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public class RegistryException extends RuntimeException {
    private static final long serialVersionUID = 2543150224511241140L;

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryException(Throwable cause) {
        super(cause);
    }
}
