package org.kinrpc.registry;

/**
 * Created by 健勤 on 2016/10/9.
 */

import org.apache.log4j.Logger;

public abstract class AbstractRegistry implements Registry{
    private final Logger logger = Logger.getLogger(AbstractRegistry.class);

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
