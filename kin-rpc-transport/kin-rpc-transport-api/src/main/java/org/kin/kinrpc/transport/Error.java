package org.kin.kinrpc.transport;

import java.io.Serializable;

/**
 * 特殊message, 包含message处理异常信息
 * @author huangjianqin
 * @date 2023/6/7
 */
public class Error implements Serializable {
    private static final long serialVersionUID = 7084462127878476170L;
    private String message;

    public Error() {
    }

    public Error(String message) {
        this.message = message;
    }

    //setter && getter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
