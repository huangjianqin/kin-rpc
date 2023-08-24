package org.kin.kinrpc.constants;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
public interface Scopes {
    /** application作用域, 即consumer和provider共享 */
    String APPLICATION = "application";
    /** consumer作用域 */
    String CONSUMER = "consumer";
    /** provider作用域 */
    String PROVIDER = "provider";
}
