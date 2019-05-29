package org.kin.kinrpc.rpc.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2016/10/9.
 */


public abstract class AbstractRegistry implements Registry {
    private final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

    protected final String address;
    protected String password;

    public AbstractRegistry(String address) {
        this.address = address;
    }

    public AbstractRegistry(String address, String password) {
        this.address = address;
        this.password = password;
    }

    public String getAddress() {
        return address;
    }


    public String getPassword() {
        return password;
    }
}
