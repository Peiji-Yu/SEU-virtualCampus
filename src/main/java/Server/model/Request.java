package Server.model;

import java.util.Map;

/**
 * 通用请求类
 * 用于接收客户端发送的JSON数据
 */
public class Request {
    private String type; // 请求类型，如 "login", "query", "control"
    private Map<String, Object> data; // 请求数据

    // 构造方法
    public Request() { this.data = new java.util.HashMap<>(); }

    public Request(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data == null ? new java.util.HashMap<>() : data;
    }

    // Getter和Setter方法
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? new java.util.HashMap<>() : data;
    }

    @Override
    public String toString() {
        return "Request{" +
                "type='" + type + '\'' +
                ", data=" + data +
                '}';
    }
}
