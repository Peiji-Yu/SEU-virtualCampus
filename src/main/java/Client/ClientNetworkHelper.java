package Client;

import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 通用客户端网络工具。
 * 职责:
 * 1. 以最小封装方式通过阻塞 Socket 将 Request(JSON) 发送至服务器并返回原始 JSON 响应字符串。
 * 2. 不做重试/超时/业务解析/线程调度，调用方需在后台线程使用并自行解析响应。
 * 设计说明:
 * - 当前未设置 socket connect/read 超时, 在网络异常/服务端无响应时线程可能长期阻塞。
 * - 调用方如需增强(超时/重试/熔断/日志) 可在外层包装；本类保持最小核心职责。
 * - 使用单次请求短连接，避免持久连接状态管理复杂度；高并发可改为连接池。
 * - 线程安全: 方法内仅使用局部变量与不可变常量，无共享可变状态。
 * @author Msgo-srAm
 */
public final class ClientNetworkHelper {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final Gson GSON = new GsonBuilder()  //增加了UUID和LocalDate的适配
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(UUID.class, new UUIDAdapter())
            .create();

    private ClientNetworkHelper() {}

    public static String send(Request request) throws IOException {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            byte[] jsonBytes = GSON.toJson(request).getBytes(StandardCharsets.UTF_8);
            dos.writeInt(jsonBytes.length);
            dos.write(jsonBytes);
            dos.flush();
            int respLen = dis.readInt();
            byte[] respBytes = new byte[respLen];
            dis.readFully(respBytes);
            return new String(respBytes, StandardCharsets.UTF_8);
        } finally {
            safeClose(dis);
            safeClose(dos);
            safeClose(socket);
        }
    }

    private static void safeClose(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}
