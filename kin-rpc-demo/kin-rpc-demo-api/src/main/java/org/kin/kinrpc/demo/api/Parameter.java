package org.kin.kinrpc.demo.api;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/7/29
 */
public class Parameter implements Serializable {
    private static final long serialVersionUID = -7261597696748732819L;
    @NotEmpty
    private String region;
    @Max(10_000)
    private int roomNumber;

    public Parameter() {
    }

    public Parameter(String region, int roomNumber) {
        this.region = region;
        this.roomNumber = roomNumber;
    }

    //setter && getter
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }
}
