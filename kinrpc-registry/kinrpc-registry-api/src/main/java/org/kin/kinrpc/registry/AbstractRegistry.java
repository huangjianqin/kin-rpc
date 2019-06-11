package org.kin.kinrpc.registry;

import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2016/10/9.
 */


public abstract class AbstractRegistry implements Registry {
    private final Logger logger = LoggerFactory.getLogger("registry");

    protected final HostAndPort address;
    protected String password;

    public AbstractRegistry(String address) {
        this(address, "");
    }

    public AbstractRegistry(String address, String password) {
        this.address = HostAndPort.fromString(address);
        this.password = password;
    }

    public HostAndPort getAddress() {
        return address;
    }


    public String getPassword() {
        return password;
    }
}
