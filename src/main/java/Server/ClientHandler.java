package Server;

import Server.model.*;
import Server.service.UserService;
import Server.service.StudentService;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客户端处理器
 * 每个客户端连接都会创建一个此类的实例
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Gson gson = new Gson();
    private final UserService userService = new UserService();
    private final StudentService studentService = new StudentService();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            System.out.println("客户端连接: " + clientSocket.getInetAddress());

            while (true) {
                // 1. 读取消息长度（4字节int）
                int length = in.readInt();
                // 2. 根据长度读取字节数组
                byte[] messageBytes = new byte[length];
                in.readFully(messageBytes);
                // 3. 将字节数组转为JSON字符串
                String jsonStr = new String(messageBytes, StandardCharsets.UTF_8);

                System.out.println("收到请求: " + jsonStr);

                // 4. 将JSON字符串解析为Request对象
                Request request = gson.fromJson(jsonStr, Request.class);

                // 5. 根据请求类型处理业务逻辑
                Response response;
                switch (request.getType()) {
                    // 用户登录
                    case "login":
                        response = userService.login(request.getData());
                        break;

                    // （学生）查询自己的学籍信息
                    case "getSelf":
                        Integer cardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        Student student = studentService.getSelf(cardNumber);
                        response = (student != null) ?
                                Response.success("查询成功", student) :
                                Response.error("未找到学生信息");
                        break;

                    // 忘记密码
                    case "forgetPwd":
                        Map<String, Object> forgetData = request.getData();

                        // 获取一卡通号和身份证号
                        Integer card_number = ((Double) forgetData.get("cardNumber")).intValue();
                        String identity = (String) forgetData.get("id");

                        // 调用忘记密码验证
                        response = userService.forgetPassword(card_number, identity);
                        break;

                    // 重置密码
                    case "resetPwd":
                        Map<String, Object> resetData = request.getData();

                        // 获取一卡通号和密码
                        Integer CardNumber = ((Double) resetData.get("cardNumber")).intValue();
                        String password = (String) resetData.get("password");

                        // 调用重置密码验证
                        response = userService.resetPassword(CardNumber, password);
                        break;

                    // 搜索学生学籍信息
                    case "searchStudents":
                        Map<String, Object> searchData = request.getData();
                        String searchType = (String) searchData.get("searchType");
                        String searchValue = (String) searchData.get("searchValue");
                        Boolean fuzzy = (Boolean) searchData.get("fuzzy"); // 新增参数，可选

                        // 验证必要参数
                        if (searchType == null || searchValue == null) {
                            response = Response.error("搜索参数不完整");
                            break;
                        }

                        try {
                            List<Student> students = studentService.searchStudents(searchType, searchValue, fuzzy);
                            response = Response.success("搜索完成", students);
                        } catch (IllegalArgumentException e) {
                            response = Response.error(e.getMessage());
                        } catch (Exception e) {
                            response = Response.error(500, "搜索过程中发生错误: " + e.getMessage());
                        }
                        break;

                    // 更新学生学籍信息
                    case "updateStudent":
                        // 从请求数据中创建Student对象
                        Map<String, Object> studentData = (Map<String, Object>) request.getData().get("student");
                        Student studentToUpdate = createStudentFromMap(studentData);

                        boolean updateResult = studentService.updateStudent(studentToUpdate);
                        response = updateResult ?
                                Response.success("更新成功") :
                                Response.error("更新失败");
                        break;

                    // 添加学生学籍信息
                    case "addStudent":
                        Map<String, Object> newStudentData = (Map<String, Object>) request.getData().get("student");
                        Student newStudent = createStudentFromMap(newStudentData);

                        boolean addResult = studentService.addStudent(newStudent);
                        response = addResult ?
                                Response.success("添加成功", newStudent.getCardNumber()) :
                                Response.error("添加失败");
                        break;

                    // 删除学生学籍信息
                    case "deleteStudent":
                        Integer deleteCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        boolean deleteResult = studentService.deleteStudent(deleteCardNumber);
                        response = deleteResult ?
                                Response.success("删除成功") :
                                Response.error("删除失败");
                        break;

                    default:
                        response = Response.error("不支持的请求类型: " + request.getType());
                        break;
                }

                // 6. 将响应对象序列化为JSON，并发送给客户端
                sendResponse(out, response);
            }
        } catch (IOException e) {
            System.out.println("客户端连接断开: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送响应给客户端
     * @param out DataOutputStream对象
     * @param response 响应对象
     * @throws IOException 如果发生I/O错误
     */
    private void sendResponse(DataOutputStream out, Response response) throws IOException {
        // 将Response对象转换为JSON字符串
        String jsonResponse = gson.toJson(response);
        // 将JSON字符串转换为字节数组
        byte[] jsonBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        // 先发送数据长度，再发送数据本身
        out.writeInt(jsonBytes.length);
        out.write(jsonBytes);
        out.flush();

        System.out.println("发送响应: " + jsonResponse);
    }

    // 添加辅助方法，用于从Map创建Student对象
    private Student createStudentFromMap(Map<String, Object> data) {
        Student student = new Student();

        if (data.containsKey("identity")) student.setIdentity((String) data.get("identity"));
        if (data.containsKey("cardNumber")) student.setCardNumber(((Double) data.get("cardNumber")).intValue());
        if (data.containsKey("studentNumber")) student.setStudentNumber((String) data.get("studentNumber"));
        if (data.containsKey("major")) student.setMajor((String) data.get("major"));
        if (data.containsKey("school")) student.setSchool((String) data.get("school"));
        if (data.containsKey("status")) student.setStatus(StudentStatus.valueOf((String) data.get("status")));
        if (data.containsKey("enrollment")) student.setEnrollment(new Date((Long) data.get("enrollment")));
        if (data.containsKey("birth")) student.setBirth(new Date((Long) data.get("birth")));
        if (data.containsKey("birthPlace")) student.setBirthPlace((String) data.get("birthPlace"));
        if (data.containsKey("politicalStat")) student.setPoliticalStat(PoliticalStatus.valueOf((String) data.get("politicalStat")));
        if (data.containsKey("gender")) student.setGender(Gender.valueOf((String) data.get("gender")));
        if (data.containsKey("name")) student.setName((String) data.get("name"));

        return student;
    }
}
