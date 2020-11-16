package org.kin.kinrpc.transport.kinrpc.serializer;

/**
 * serializer type冲突异常
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializerTypeConflictException extends RuntimeException {
    public SerializerTypeConflictException(int type,
                                           Class<? extends Serializer> class1,
                                           Class<? extends Serializer> class2) {
        super(String.format("serializer type conflict! type: %s, serializer: %s, %s", type, class1.getName(), class2.getName()));
    }
}
