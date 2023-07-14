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

    /** 不指向任何actor的{@link ActorAddress}实例 */
    public static final ActorAddress NO_SENDER = of(Address.of("", -1), "");

    /** address */
    private Address address;
    /** actor name */
    private String name;

    private ActorAddress() {
    }

    //------------------------------------------------------------------------------------------------------------
    public static ActorAddress of(Address address, String name) {
        ActorAddress actorAddress = new ActorAddress();
        actorAddress.address = address;
        actorAddress.name = name;
        return actorAddress;
    }

    public static ActorAddress of(String name) {
        return of(Address.LOCAL, name);
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
