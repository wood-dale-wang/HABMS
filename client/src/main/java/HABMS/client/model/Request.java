package HABMS.client.model;

/**
 * 发送到服务器的统一请求封装。
 */
public class Request {
    private String type;
    private Object data;

    public Request() {}

    public Request(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
