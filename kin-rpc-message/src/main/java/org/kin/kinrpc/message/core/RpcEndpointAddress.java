package org.kin.kinrpc.message.core;

import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;

import java.io.Serializable;
import java.util.Objects;

/**
 * RpcEndpoint地址
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
public final class RpcEndpointAddress implements Serializable {
    private static final long serialVersionUID = 5376277467578311383L;

    /** rpc地址 */
    private KinRpcAddress rpcAddress;
    /** receiver name */
    private String name;

    //------------------------------------------------------------------------------------------------------------
    public static RpcEndpointAddress of(KinRpcAddress rpcAddress, String name) {
        RpcEndpointAddress endpointAddress = new RpcEndpointAddress();
        endpointAddress.rpcAddress = rpcAddress;
        endpointAddress.name = name;
        return endpointAddress;
    }

    //------------------------------------------------------------------------------------------------------------
    public KinRpcAddress getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(KinRpcAddress rpcAddress) {
        this.rpcAddress = rpcAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcEndpointAddress that = (RpcEndpointAddress) o;
        return Objects.equals(rpcAddress, that.rpcAddress) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rpcAddress, name);
    }

    @Override
    public String toString() {
        return "RpcEndpointAddress{" +
                "rpcAddress=" + rpcAddress.schema() +
                ", name='" + name + '\'' +
                '}';
    }
}
