package Server.model;

/**
 * 通用响应类
 * 用于向客户端返回JSON格式的响应
 */
public class Response {
    private int code;    // 状态码：200成功，400客户端错误，500服务器错误
    private String message; // 响应消息
    private Object data; // 响应数据
    private boolean success; // 兼容老前端，表示请求是否成功（code==200）

    // 构造方法
    public Response() {}

    public Response(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = (this.code == 200);
    }

    // 成功响应的快捷方法
    public static Response success(String message, Object data) {
        return new Response(200, message, data);
    }

    public static Response success(String message) {
        return success(message, null);
    }

    // 错误响应的快捷方法
    public static Response error(int code, String message) {
        return new Response(code, message, null);
    }

    public static Response error(String message) {
        return error(400, message);
    }

    // Getter和Setter方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
        this.success = (this.code == 200);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", success=" + success +
                '}';
    }
}
