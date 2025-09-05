package Client;

import Server.model.Request;
import Server.model.student.*;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 与服务器交互的学生客户端服务，封装网络与数据转换逻辑。
 */
public class StudentClientService {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private final Gson gson = new Gson();

    // ===================== 基础网络发送方法 =====================
    private Map<String, Object> sendRequest(String action, Map<String, Object> data) throws Exception {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            Request request = new Request(action, data);
            String json = gson.toJson(request);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

            int len = dis.readInt();
            byte[] respBytes = new byte[len];
            dis.readFully(respBytes);
            String resp = new String(respBytes, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = gson.fromJson(resp, Map.class);
            return respMap;
        } finally {
            if (dis != null) try { dis.close(); } catch (IOException ignored) {}
            if (dos != null) try { dos.close(); } catch (IOException ignored) {}
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // ===================== 对外功能 =====================
    public Student getSelf(int cardNumber) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        Map<String, Object> resp = sendRequest("getSelf", data);
        if (isOk(resp)) {
            @SuppressWarnings("unchecked") Map<String, Object> stuMap = (Map<String, Object>) resp.get("data");
            return convertMapToStudent(stuMap);
        }
        return null;
    }

    public List<Student> searchStudents(String searchType, String searchValue, boolean fuzzy) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("searchType", searchType);
        data.put("searchValue", searchValue);
        data.put("fuzzy", fuzzy);
        Map<String, Object> resp = sendRequest("searchStudents", data);
        if (isOk(resp)) {
            @SuppressWarnings("unchecked") List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("data");
            return list.stream().map(this::convertMapToStudent).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public boolean addStudent(Student student) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("student", convertStudentToMap(student));
        Map<String, Object> resp = sendRequest("addStudent", data);
        return isOk(resp);
    }

    public boolean updateStudent(Student student) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("student", convertStudentToMap(student));
        Map<String, Object> resp = sendRequest("updateStudent", data);
        return isOk(resp);
    }

    public boolean deleteStudent(int cardNumber) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        Map<String, Object> resp = sendRequest("deleteStudent", data);
        return isOk(resp);
    }

    private boolean isOk(Map<String, Object> resp) {
        return resp != null && resp.get("code") != null && ((Double) resp.get("code")).intValue() == 200;
    }

    // ===================== 数据转换 =====================
    private Map<String, Object> convertStudentToMap(Student s) {
        Map<String, Object> m = new HashMap<>();
        if (s.getCardNumber() != null) m.put("cardNumber", s.getCardNumber());
        if (s.getName() != null) m.put("name", s.getName());
        if (s.getIdentity() != null) m.put("identity", s.getIdentity());
        if (s.getStudentNumber() != null) m.put("studentNumber", s.getStudentNumber());
        if (s.getMajor() != null) m.put("major", s.getMajor());
        if (s.getSchool() != null) m.put("school", s.getSchool());
        if (s.getBirthPlace() != null) m.put("birthPlace", s.getBirthPlace());
        if (s.getGender() != null) m.put("gender", s.getGender().name());
        if (s.getStatus() != null) m.put("status", s.getStatus().name());
        if (s.getPoliticalStat() != null) m.put("politicalStat", s.getPoliticalStat().name());
        if (s.getBirth() != null) m.put("birth", s.getBirth().getTime());
        if (s.getEnrollment() != null) m.put("enrollment", s.getEnrollment().getTime());
        return m;
    }

    public Student convertMapToStudent(Map<String, Object> data) {
        Student student = new Student();
        if (data == null) return student;
        if (data.get("cardNumber") != null) student.setCardNumber(((Double) data.get("cardNumber")).intValue());
        if (data.get("name") != null) student.setName((String) data.get("name"));
        if (data.get("identity") != null) student.setIdentity((String) data.get("identity"));
        if (data.get("studentNumber") != null) student.setStudentNumber((String) data.get("studentNumber"));
        if (data.get("major") != null) student.setMajor((String) data.get("major"));
        if (data.get("school") != null) student.setSchool((String) data.get("school"));
        if (data.get("birthPlace") != null) student.setBirthPlace((String) data.get("birthPlace"));
        if (data.get("gender") != null) try { student.setGender(Gender.valueOf((String) data.get("gender"))); } catch (Exception ignored) {}
        if (data.get("status") != null) try { student.setStatus(StudentStatus.valueOf((String) data.get("status"))); } catch (Exception ignored) {}
        if (data.get("politicalStat") != null) try { student.setPoliticalStat(PoliticalStatus.valueOf((String) data.get("politicalStat"))); } catch (Exception ignored) {}
        if (data.get("birth") != null) student.setBirth(parseDateFlex(data.get("birth")));
        if (data.get("enrollment") != null) student.setEnrollment(parseDateFlex(data.get("enrollment")));
        return student;
    }

    private Date parseDateFlex(Object obj) {
        try {
            if (obj instanceof Number) return new Date(((Number) obj).longValue());
            if (obj instanceof String) return parseDate((String) obj);
        } catch (Exception e) {
            System.err.println("日期解析失败: " + obj + " => " + e.getMessage());
        }
        return null;
    }

    // 服务器返回示例: "Sep 2, 2025, 12:00:00 AM" 可能包含窄不换行空格 U+202F
    public Date parseDate(String dateStr) throws Exception {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.replace('\u202F', ' '); // 替换窄不换行空格
        dateStr = dateStr.replaceAll("\\p{Z}+", " ").trim();

        String[] formats = {
                "MMM d, yyyy, h:mm:ss a",
                "MMM dd, yyyy, h:mm:ss a",
                "MMM d, yyyy, hh:mm:ss a",
                "MMM dd, yyyy, hh:mm:ss a",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy"
        };
        for (String f : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.ENGLISH);
                sdf.setLenient(false);
                return sdf.parse(dateStr);
            } catch (Exception ignored) {}
        }
        throw new Exception("无法解析日期: " + dateStr);
    }
}

