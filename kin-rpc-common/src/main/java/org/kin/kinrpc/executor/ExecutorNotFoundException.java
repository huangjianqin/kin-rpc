package org.kin.kinrpc.executor;

/**
 * @author huangjianqin
 * @date 2023/7/5
 */
public class ExecutorNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 529101299719085032L;

    public ExecutorNotFoundException(String name) {
        super(String.format("can not find executor named '%s'. please ensure executor has been registered before use", name));
    }
}
