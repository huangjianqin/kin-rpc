package org.kin.kinrpc.transport.kinrpc;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
public class KinRpcAddress implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(KinRpcAddress.class);
    private static final long serialVersionUID = 145033673996597409L;

    private String host;
    private int port;

    public KinRpcAddress() {
    }

    //------------------------------------------------------------------------------------------------------------
    public static KinRpcAddress of(String host, int port) {
        KinRpcAddress address = new KinRpcAddress();
        address.host = host;
        address.port = port;
        return address;
    }

    public static KinRpcAddress of(String uriStr) {
        try {
            URI uri = new URI(uriStr);
            KinRpcAddress address = new KinRpcAddress();
            address.host = uri.getHost();
            address.port = uri.getPort();
            return address;
        } catch (URISyntaxException e) {
            ExceptionUtils.throwExt(e);
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------
    public String address() {
        return NetUtils.getIpPort(host, port);
    }

    //------------------------------------------------------------------------------------------------------------
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
        KinRpcAddress that = (KinRpcAddress) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "RpcAddress{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
