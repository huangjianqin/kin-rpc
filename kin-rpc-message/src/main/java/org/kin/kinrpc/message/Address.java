package org.kin.kinrpc.message;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
public final class Address implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Address.class);
    private static final long serialVersionUID = 145033673996597409L;

    /** {@link Address} for internal jvm address */
    public static final Address LOCAL = Address.of("", -1);

    /** host name */
    private String host;
    /** port */
    private int port;

    public Address() {
    }

    //------------------------------------------------------------------------------------------------------------
    public static Address of(int port) {
        return of(NetUtils.getLocalhost4Ip(), port);
    }

    public static Address of(String host, int port) {
        Address address = new Address();
        address.host = host;
        address.port = port;
        return address;
    }

    public static Address of(String uriStr) {
        try {
            URI uri = new URI(uriStr);
            Address address = new Address();
            address.host = uri.getHost();
            address.port = uri.getPort();
            return address;
        } catch (URISyntaxException e) {
            ExceptionUtils.throwExt(e);
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------

    /** 返回是否local */
    public boolean isLocal() {
        return StringUtils.isBlank(host) && port < 1;
    }

    //setter && getter
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address that = (Address) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return NetUtils.getIpPort(host, port);
    }
}