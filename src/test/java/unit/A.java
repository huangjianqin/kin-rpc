package unit;

import java.io.Serializable;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class A implements Serializable {
    private String state = "";

    public A(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "unit.A{" +
                "state='" + state + '\'' +
                '}';
    }
}
