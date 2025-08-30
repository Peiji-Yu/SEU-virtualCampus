package java;

import com.google.gson.Gson;
import Server.model.Request;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientTest {
    public static void main(String[] args) {
        // 服务器地址和端口
        String host = "localhost";
        int port = 8888;

        // 创建Gson实例
        Gson gson = new Gson();

        // 1. 构建请求数据 (使用测试数据库中的账号)
        Map<String, Object> data = new HashMap<>();
        data.put("username", "student1");
        data.put("password", "e10adc3949ba59abbe56e057f20f883e"); // "123456"的MD5

        Request request = new Request("login", data);

        // 2. 将请求对象转换为JSON字符串
        String jsonRequest = gson.toJson(request);
        System.out.println("准备发送: " + jsonRequest);

        // 3. 转换为字节数组并计算长度
        byte[] jsonBytes = jsonRequest.getBytes(StandardCharsets.UTF_8);
        int length = jsonBytes.length;

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("已连接到服务器...");

            // 4. 先发送数据长度（4字节的int）
            out.writeInt(length);
            // 5. 再发送JSON数据本身
            out.write(jsonBytes);
            out.flush();

            System.out.println("请求已发送，等待响应...");
            // 这里可以添加读取服务器响应的代码

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}