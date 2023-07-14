package org.kin.kinrpc.demo.message;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public class PrintMessage implements Serializable {
    private static final long serialVersionUID = -1632194863001778858L;

    private String content;

    public PrintMessage() {
    }

    public PrintMessage(String content) {
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
        return "PrintMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}