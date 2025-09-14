package Client;

import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Type;

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

    /**
     * 一卡通挂失请求
     * @param cardNumber 一卡通号
     * @return 服务器返回结果
     */
    public static String reportLoss(String cardNumber) throws IOException {
        // 构造 JSON 请求
        String json = "{\"type\":\"reportLoss\",\"data\":{\"cardNumber\":" + cardNumber + "}}";
        Request req = GSON.fromJson(json, Request.class);
        return send(req);
    }

    /**
     * 查询所有挂失一卡通账号
     * @return 服务器返回结果Map
     */
    public static Map<String, Object> findAllLostCards() {
        try {
            String json = "{\"type\":\"findAllLostCards\",\"data\":null}";
            Request req = GSON.fromJson(json, Request.class);
            String resp = send(req);
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            return (Map<String, Object>) GSON.fromJson(resp, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 管理员解除挂失
     * @param targetCardNumber 要解除挂失的一卡通号
     * @return 服务器返回结果Map
     */
    public static Map<String, Object> cancelReportLoss(long targetCardNumber) {
        try {
            String json = String.format("{\"type\":\"cancelReportLoss\",\"data\":{\"targetCardNumber\":%d}}", targetCardNumber);
            Request req = GSON.fromJson(json, Request.class);
            String resp = send(req);
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            return (Map<String, Object>) GSON.fromJson(resp, type);
        } catch (Exception e) {
            return null;
        }
    }

    // 课程管理相关接口
    public static String getAllCourses() throws IOException {
        Request req = new Request();
        req.setType("getAllCourses");
        // 避免 data 为 null 导致某些序列化/解析场景不一致，使用空 Map
        Map<String, Object> data = new HashMap<>();
        req.setData(data);
        return send(req);
    }
    public static String getCourseById(String courseId) throws IOException {
        Request req = new Request();
        req.setType("getCourseById");
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        req.setData(data);
        return send(req);
    }
    public static String getCourseByName(String courseName) throws IOException {
        Request req = new Request();
        req.setType("getCourseByName");
        Map<String, Object> data = new HashMap<>();
        data.put("courseName", courseName);
        req.setData(data);
        return send(req);
    }
    public static String getCourseBySchool(String school) throws IOException {
        Request req = new Request();
        req.setType("getCourseBySchool");
        Map<String, Object> data = new HashMap<>();
        data.put("school", school);
        req.setData(data);
        return send(req);
    }
    public static String addCourse(String courseId, String courseName, String school, float credit) throws IOException {
        Request req = new Request();
        req.setType("addCourse");
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        data.put("courseName", courseName);
        data.put("school", school);
        data.put("credit", credit);
        req.setData(data);
        return send(req);
    }
    public static String updateCourse(String courseId, String courseName, String school, float credit) throws IOException {
        Request req = new Request();
        req.setType("updateCourse");
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        data.put("courseName", courseName);
        data.put("school", school);
        data.put("credit", credit);
        req.setData(data);
        return send(req);
    }
    public static String deleteCourse(String courseId) throws IOException {
        Request req = new Request();
        req.setType("deleteCourse");
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        req.setData(data);
        return send(req);
    }

    // 获取教师教学班
    public static String getTeachingClassesByTeacherName(String teacherName) throws IOException {
        String json = String.format("{\"type\":\"getTeachingClassesByTeacherName\",\"data\":{\"teacherName\":\"%s\"}}", teacherName);
        Request req = GSON.fromJson(json, Request.class);
        return send(req);
    }

    // 获取教学班学生列表
    public static String getTeachingClassStudents(String teachingClassUuid) throws IOException {
        String json = String.format("{\"type\":\"getTeachingClassStudents\",\"data\":{\"teachingClassUuid\":\"%s\"}}", teachingClassUuid);
        Request req = GSON.fromJson(json, Request.class);
        return send(req);
    }

    // 选课相关接口
    public static String getTeachingClassesByCourseId(String courseId) throws IOException {
        Request req = new Request();
        req.setType("getTeachingClassesByCourseId");
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        req.setData(data);
        return send(req);
    }
    public static String getStudentSelectedCourses(String cardNumber) throws IOException {
        Request req = new Request();
        req.setType("getStudentSelectedCourses");
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        req.setData(data);
        return send(req);
    }
    public static String dropCourse(String cardNumber, String courseId) throws IOException {
        Request req = new Request();
        req.setType("dropCourse");
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("courseId", courseId);
        req.setData(data);
        return send(req);
    }
    public static String selectCourse(String cardNumber, String courseId) throws IOException {
        Request req = new Request();
        req.setType("selectCourse");
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("courseId", courseId);
        req.setData(data);
        return send(req);
    }

    // 获取所有教学班
    public static String getAllTeachingClasses() throws IOException {
        Request req = new Request();
        req.setType("getAllTeachingClasses");
        Map<String, Object> data = new HashMap<>();
        req.setData(data);
        return send(req);
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
