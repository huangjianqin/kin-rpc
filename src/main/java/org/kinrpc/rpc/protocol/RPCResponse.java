package org.kinrpc.rpc.protocol;

import java.io.Serializable;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RPCResponse implements Serializable {
    private int requestId;
    private Object result;
    private String info;
    private State state;

    public enum State{
        ERROR(-1), SUCCESS(1), RETRY(0);

        int code;

        State(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public RPCResponse(){

    }

    public RPCResponse(int requestId) {
        this.requestId = requestId;
    }

    public void setState(State state, String info){
        this.state = state;
        this.info = info;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RPCResponse)) return false;

        RPCResponse that = (RPCResponse) o;

        return requestId == that.requestId;

    }

    @Override
    public int hashCode() {
        return requestId;
    }

    @Override
    public String toString() {
        return "RPCResponse{" +
                "requestId=" + requestId +
                ", info='" + info + '\'' +
                ", state=" + state +
                '}';
    }
}
