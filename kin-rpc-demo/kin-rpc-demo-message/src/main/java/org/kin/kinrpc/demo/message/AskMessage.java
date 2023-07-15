package org.kin.kinrpc.demo.message;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public class AskMessage implements Serializable {
    private static final long serialVersionUID = -6807586672826372145L;

    private String content;

    public AskMessage() {
    }

    public AskMessage(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "AskMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}