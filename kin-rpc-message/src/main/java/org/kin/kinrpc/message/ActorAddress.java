package org.kin.kinrpc.message;

import java.io.Serializable;
import java.util.Objects;

/**
 * actor name + actor address
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
public final class ActorAddress implements Serializable {
    private static final long serialVersionUID = 5376277467578311383L;

    /** actor address */
    private Address address;
    /** actor name */
    private String name;

    //------------------------------------------------------------------------------------------------------------
    public static ActorAddress of(Address address, String name) {
        ActorAddress endpointAddress = new ActorAddress();
        endpointAddress.address = address;
        endpointAddress.name = name;
        return endpointAddress;
    }

    //------------------------------------------------------------------------------------------------------------
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
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
        ActorAddress that = (ActorAddress) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, name);
    }

    @Override
    public String toString() {
        return "ActorAddress{" +
                "address=" + address +
                ", name='" + name + '\'' +
                '}';
    }
}
