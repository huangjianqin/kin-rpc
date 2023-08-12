package org.kin.kinrpc.registry;

/**
 * @author huangjianqin
 * @date 2023/8/12
 */
public class RegistryException extends RuntimeException {
    private static final long serialVersionUID = -7340591201684118249L;

    public RegistryException() {
    }

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryException(Throwable cause) {
        super(cause);
    }

    public RegistryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
