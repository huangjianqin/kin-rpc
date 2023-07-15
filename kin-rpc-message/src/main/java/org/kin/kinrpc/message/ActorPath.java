package org.kin.kinrpc.message;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * actor name + address
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
public final class ActorPath implements Serializable {
    private static final long serialVersionUID = 5376277467578311383L;

    /** 不指向任何actor的{@link ActorPath}实例 */
    public static final ActorPath NO_SENDER = of(Address.LOCAL, "");

    /** address */
    private Address address;
    /** actor name */
    private String name;

    public ActorPath() {
    }

    public ActorPath(Address address, String name) {
        Preconditions.checkNotNull(address);
        this.address = address;
        this.name = name;
    }

    //------------------------------------------------------------------------------------------------------------
    public static ActorPath of(Address address, String name) {
        return new ActorPath(address, name);
    }

    public static ActorPath of(String name) {
        return of(Address.LOCAL, name);
    }

    //------------------------------------------------------------------------------------------------------------

    /** 返回是否没有sender */
    public boolean isNoSender() {
        return StringUtils.isBlank(name);
    }

    //setter && getter
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
        ActorPath that = (ActorPath) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, name);
    }

    @Override
    public String toString() {
        return "ActorPath{" +
                "address=" + address +
                ", name='" + name + '\'' +
                '}';
    }
}
