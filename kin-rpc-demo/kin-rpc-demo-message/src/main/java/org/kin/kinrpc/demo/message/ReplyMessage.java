package org.kin.kinrpc.demo.message;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public class ReplyMessage implements Serializable {
    private static final long serialVersionUID = -1586292592951384110L;
    private String content;

    public ReplyMessage() {
    }

    public ReplyMessage(String content) {
        this.content = content;
    }

    //setter && getter
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ReplyMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}