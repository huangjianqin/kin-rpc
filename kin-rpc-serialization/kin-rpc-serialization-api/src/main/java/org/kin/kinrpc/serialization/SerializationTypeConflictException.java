package org.kin.kinrpc.serialization;

/**
 * serialization type冲突异常
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializationTypeConflictException extends RuntimeException {
    private static final long serialVersionUID = 4102974906292170702L;

    public SerializationTypeConflictException(int type,
                                              Class<? extends Serialization> class1,
                                              Class<? extends Serialization> class2) {
        super(String.format("serialization type conflict! type: %s, serialization: %s, %s", type, class1.getName(), class2.getName()));
    }
}
