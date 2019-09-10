package org.kin.kinrpc.registry.exception;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class AddressFormatErrorException extends RuntimeException {
    public AddressFormatErrorException(String address) {
        super("url address format error >>> " + address);
    }
}
