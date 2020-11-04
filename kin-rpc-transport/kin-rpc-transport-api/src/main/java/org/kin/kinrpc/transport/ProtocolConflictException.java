package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
public class ProtocolConflictException extends RuntimeException {
    private static final long serialVersionUID = -6703540984000817966L;

    public ProtocolConflictException(Class<? extends Protocol> class1,
                                     Class<? extends Protocol> class2) {
        super(String.format("protocol name conflict! %s, %s", class1.getName(), class2.getName()));
    }
}

