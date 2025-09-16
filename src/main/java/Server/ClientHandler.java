package Server;

import Server.model.*;

import Server.model.login.User;
import Server.model.book.*;
import Server.model.shop.*;
import Server.model.student.Gender;
import Server.model.student.PoliticalStatus;
import Server.model.student.Student;
import Server.model.student.StudentStatus;
import Server.service.login.UserService;
import Server.service.student.StudentService;
import Server.service.shop.FinanceService;
import Server.service.shop.StoreService;
import Server.dao.shop.StoreMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import Server.model.book.Category;
import Server.model.course.Course;
import Server.model.course.TeachingClass;
import Server.model.course.ClassStudent;
import Server.model.course.StudentTeachingClass;
import Server.service.course.ClassStudentService;
import Server.service.course.CourseService;
import Server.service.course.TeachingClassService;
import Server.service.course.StudentTeachingClassService;
import Server.service.book.BookService;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 客户端处理器
 * 每个客户端连接都会创建一个此类的实例
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final TypeAdapter<LocalDateTime> LOCAL_DATE_TIME_ADAPTER = new TypeAdapter<LocalDateTime>() {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.toString());
        }
        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            String s = in.nextString();
            return s == null || s.isEmpty() ? null : LocalDateTime.parse(s);
        }
    };
    // 新增 LocalDate 适配器
    private static final TypeAdapter<LocalDate> LOCAL_DATE_ADAPTER = new TypeAdapter<LocalDate>() {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.toString());
        }
        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            String s = in.nextString();
            return s == null || s.isEmpty() ? null : LocalDate.parse(s);
        }
    };
    // 通用 java.time.* 兜底序列化（只写）— 防止 Gson 继续反射访问 JDK 内部字段
    private static final TypeAdapterFactory JAVA_TIME_FALLBACK_FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<? super T> raw = type.getRawType();
            if (raw.getName().startsWith("java.time.")) {
                return new TypeAdapter<T>() {
                    @Override public void write(JsonWriter out, T value) throws IOException {
                        if (value == null) { out.nullValue(); return; }
                        out.value(value.toString());
                    }
                    @Override public T read(JsonReader in) throws IOException { // 仅在反序列化需要时尝试处理最常用类型
                        if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
                        String s = in.nextString();
                        // 针对常用类型处理，其它直接返回 null (当前场景仅服务端输出，不影响)
                        try {
                            if (raw == LocalDateTime.class) return (T) LocalDateTime.parse(s);
                            if (raw == LocalDate.class) return (T) LocalDate.parse(s);
                        } catch (Exception ignored) { }
                        return null; // 兜底
                    }
                };
            }
            return null;
        }
    };

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATE_TIME_ADAPTER)
            .registerTypeAdapter(LocalDate.class, LOCAL_DATE_ADAPTER)
            .registerTypeAdapterFactory(JAVA_TIME_FALLBACK_FACTORY)
            .serializeNulls()
            .create();
    private final UserService userService = new UserService();
    private final StudentService studentService = new StudentService();
    private final ClassStudentService classStudentService = new ClassStudentService();
    private final CourseService courseService = new CourseService();
    private final TeachingClassService teachingClassService = new TeachingClassService();
    private final StudentTeachingClassService studentTeachingClassService = new StudentTeachingClassService();
    private final StoreService storeService = new StoreService();
    private final FinanceService financeService = new FinanceService();
    private final BookService bookService = new BookService();
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
                Response response = null;
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

                    // 获取所有课程
                    case "getAllCourses":
                        List<Course> courses = courseService.getAllCourses();
                        response = Response.success("获取所有课程成功", courses);
                        break;

                    // 根据 courseId 获取单条课程详情（客户端需要的接口）
                    case "getCourseById":
                        if (request.getData() == null) {
                            response = Response.error("缺少参数: courseId");
                            break;
                        }
                        Object cidObj = request.getData().get("courseId");
                        String queryCourseId = null;
                        if (cidObj instanceof String) queryCourseId = (String) cidObj;
                        else if (cidObj != null) queryCourseId = String.valueOf(cidObj);

                        if (queryCourseId == null || queryCourseId.trim().isEmpty()) {
                            response = Response.error("缺少参数: courseId");
                            break;
                        }

                        try {
                            Course course = courseService.findByCourseId(queryCourseId);
                            if (course != null) {
                                response = Response.success("获取课程成功", course);
                            } else {
                                response = Response.error("未找到课程: " + queryCourseId);
                            }
                        } catch (Exception e) {
                            response = Response.error(500, "查询课程失败: " + e.getMessage());
                        }
                        break;

                    // 根据学院查询课程
                    case "getCoursesBySchool":
                        String school = (String) request.getData().get("school");
                        List<Course> schoolCourses = courseService.getCoursesBySchool(school);
                        response = Response.success("获取学院课程成功", schoolCourses);
                        break;

                    // 获取所在教学班
                    case "getAllTeachingClasses":
                        List<TeachingClass> teachingClasses = teachingClassService.getAllTeachingClasses();
                        response = Response.success("获取所有教学班成功", teachingClasses);
                        break;

                    // 根据课程ID获取教学班
                    case "getTeachingClassesByCourseId":
                        String courseId = (String) request.getData().get("courseId");
                        List<TeachingClass> courseTeachingClasses = teachingClassService.findByCourseId(courseId);
                        response = Response.success("获取课程教学班成功", courseTeachingClasses);
                        break;

                    // 根据教师姓名获取教学班
                    case "getTeachingClassesByTeacherName":
                        String teacherName = (String) request.getData().get("teacherName");
                        List<TeachingClass> teacherTeachingClasses = teachingClassService.getTeachingClassesByTeacherName(teacherName);
                        response = Response.success("获取教师教学班成功", teacherTeachingClasses);
                        break;

                    // 根据一卡通号查询教师负责的教学班（服务端负责将 cardNumber -> 用户姓名 -> 查询教学班）
                    case "getTeachingClassesByTeacherCardNumber":
                        try {
                            Object cardObj = request.getData().get("cardNumber");
                            Integer queryCard = null;
                            if (cardObj instanceof Number) {
                                queryCard = ((Number) cardObj).intValue();
                            } else if (cardObj instanceof String) {
                                try { queryCard = Integer.parseInt(((String) cardObj).trim()); } catch (NumberFormatException ignored) {}
                            }
                            if (queryCard == null) {
                                response = Response.error("无效的 cardNumber 参数");
                                break;
                            }
                            // 查用户以取得姓名
                            User user = userService.findUserByCardNumber(queryCard);
                            if (user == null || user.getName() == null || user.getName().trim().isEmpty()) {
                                response = Response.error("未找到对应用户或姓名为空");
                                break;
                            }
                            String realTeacherName = user.getName();
                            List<TeachingClass> classes = teachingClassService.getTeachingClassesByTeacherName(realTeacherName);
                            response = Response.success("获取教师教学班成功", classes);
                        } catch (Exception e) {
                            response = Response.error(500, "根据一卡通号查询教学班失败: " + e.getMessage());
                        }
                        break;

                    // 学生选课
                    case "selectCourse":
                        Integer selectCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        String teachingClassUuid = (String) request.getData().get("teachingClassUuid");

                        try {
                            // 时间冲突检测：获取目标教学班的 schedule，与学生已选教学班逐一对比
                            TeachingClass targetTcForConflict = teachingClassService.findByUuid(teachingClassUuid);
                            if (targetTcForConflict != null) {
                                List<StudentTeachingClass> existingSelections = studentTeachingClassService.findByStudentCardNumber(selectCardNumber);
                                if (existingSelections != null) {
                                    for (StudentTeachingClass stc : existingSelections) {
                                        if (stc == null || stc.getTeachingClassUuid() == null) continue;
                                        TeachingClass existTc = teachingClassService.findByUuid(stc.getTeachingClassUuid());
                                        if (existTc == null) continue;
                                        if (schedulesConflict(existTc.getSchedule(), targetTcForConflict.getSchedule())) {
                                            response = Response.error("选课失败: 与已选课程时间冲突");
                                            break;
                                        }
                                    }
                                    if (response != null && response.getCode() != 200) break; // 已设置冲突响应，跳出
                                }
                            }
                             // 检查教学班是否有空位
                             boolean hasSeats = teachingClassService.hasAvailableSeats(teachingClassUuid);
                             if (!hasSeats) {
                                 response = Response.error("教学班已满，无法选课");
                                 break;
                             }

                             // 检查是否已经选过该课程
                             boolean alreadySelected = studentTeachingClassService.findByStudentAndTeachingClass(selectCardNumber, teachingClassUuid) != null;
                             if (alreadySelected) {
                                 response = Response.error("您已经选过该课程");
                                 break;
                             }

                             // 创建选课关系
                             boolean selectResult = studentTeachingClassService.addStudentTeachingClass(
                                     new StudentTeachingClass(selectCardNumber, teachingClassUuid));

                             if (selectResult) {
                                 // 更新教学班选课人数
                                 teachingClassService.incrementSelectedCount(teachingClassUuid);
                                 response = Response.success("选课成功");
                             } else {
                                 response = Response.error("选课失败");
                             }
                         } catch (Exception e) {
                             response = Response.error("选课过程中发生错误: " + e.getMessage());
                         }
                         break;

                    // 学生退课
                    case "dropCourse":
                        Integer dropCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        String dropTeachingClassUuid = (String) request.getData().get("teachingClassUuid");

                        try {
                            // 检查是否选过该课程
                            boolean isSelected = studentTeachingClassService.findByStudentAndTeachingClass(dropCardNumber, dropTeachingClassUuid) != null;
                            if (!isSelected) {
                                response = Response.error("您没有选过该课程");
                                break;
                            }

                            // 删除选课关系
                            boolean dropResult = studentTeachingClassService.deleteStudentTeachingClass(dropCardNumber, dropTeachingClassUuid);

                            if (dropResult) {
                                // 更新教学班选课人数
                                teachingClassService.decrementSelectedCount(dropTeachingClassUuid);
                                response = Response.success("退课成功");
                            } else {
                                response = Response.error("退课失败");
                            }
                        } catch (Exception e) {
                            response = Response.error("退课过程中发生错误: " + e.getMessage());
                        }
                        break;

                    // 获取学生已选课程
                    case "getStudentSelectedCourses":
                        Integer studentCardNumber = ((Double) request.getData().get("cardNumber")).intValue();

                        try {
                            // 使用一次性 JOIN 查询获取该学生所有已选教学班，避免逐条查询导致的 N+1 问题
                            List<TeachingClass> teachingClasses1 = teachingClassService.findByStudentCardNumber(studentCardNumber);
                            response = Response.success("获取已选课程成功", teachingClasses1);
                        } catch (Exception e) {
                            response = Response.error("获取已选课程失败: " + e.getMessage());
                        }
                        break;

                    // 获取教学班的学生列表
                    case "getTeachingClassStudents":
                        String classUuid = (String) request.getData().get("teachingClassUuid");

                        try {
                            // 获取教学班的选课关系
                            List<StudentTeachingClass> classStudents = studentTeachingClassService.findByTeachingClassUuid(classUuid);

                            // 获取学生详细信息
                            List<Map<String, Object>> students = new ArrayList<>();
                            for (StudentTeachingClass stc : classStudents) {
                                ClassStudent student1 = classStudentService.findByCardNumber(stc.getStudentCardNumber());
                                if (student1 != null) {
                                    Map<String, Object> m = new HashMap<>();
                                    m.put("cardNumber", student1.getCardNumber());
                                    // 强制将学号按字符串返回，保留前导零，若为数字则左补0到8位
                                    Object stuNoObj = null;
                                    try { stuNoObj = student1.getStudentNumber(); } catch (Exception ignore) {}
                                    String stuNoStr = "";
                                    if (stuNoObj == null) {
                                        stuNoStr = "";
                                    } else if (stuNoObj instanceof String) {
                                        stuNoStr = (String) stuNoObj;
                                    } else if (stuNoObj instanceof Number) {
                                        // 转为整数并左补零
                                        long val = ((Number) stuNoObj).longValue();
                                        stuNoStr = String.format("%08d", val);
                                    } else {
                                        stuNoStr = String.valueOf(stuNoObj);
                                    }
                                    m.put("studentNumber", stuNoStr);
                                    m.put("major", student1.getMajor());
                                    m.put("school", student1.getSchool());
                                    m.put("status", student1.getStatus());
                                    // 返回学生姓名而不是 selectedClasses
                                    m.put("name", student1.getName());
                                    students.add(m);
                                }
                            }

                            response = Response.success("获取教学班学生列表成功", students);
                        } catch (Exception e) {
                            response = Response.error("获取教学班学生列表失败: " + e.getMessage());
                        }
                        break;

                    // 添加课程（管理员功能）
                    case "addCourse":
                        Map<String, Object> courseData;
                        if (request.getData().containsKey("course")) {
                            courseData = (Map<String, Object>) request.getData().get("course");
                        } else {
                            courseData = request.getData();
                        }
                        Course newCourse = createCourseFromMap(courseData);

                        boolean addCourseResult = courseService.addCourse(newCourse);
                        response = addCourseResult ?
                                Response.success("添加课程成功") :
                                Response.error("添加课程失败");
                        break;

                    // 更新课程（部分更新）
                    case "updateCourse":
                        String updateCourseId = (String) request.getData().get("courseId");
                        Map<String, Object> courseUpdates;
                        if (request.getData().containsKey("updates")) {
                            courseUpdates = (Map<String, Object>) request.getData().get("updates");
                        } else {
                            courseUpdates = request.getData();
                        }
                        // 获取现有课程信息
                        Course existingCourse = courseService.findByCourseId(updateCourseId);
                        if (existingCourse == null) {
                            response = Response.error("课程不存在");
                            break;
                        }
                        // 应用更新 - 检查并更新所有可能的字段
                        if (courseUpdates.containsKey("courseName")) {
                            existingCourse.setCourseName((String) courseUpdates.get("courseName"));
                        }
                        if (courseUpdates.containsKey("school")) {
                            existingCourse.setSchool((String) courseUpdates.get("school"));
                        }
                        if (courseUpdates.containsKey("credit")) {
                            existingCourse.setCredit(((Double) courseUpdates.get("credit")).floatValue());
                        }

                        // 保存更新
                        boolean updateCourseResult = courseService.updateCourse(existingCourse);
                        response = updateCourseResult ?
                                Response.success("更新课程成功") :
                                Response.error("更新课程失败");
                        break;

                    // 删除课程（管理员功能）
                    case "deleteCourse":
                        String deleteCourseId = (String) request.getData().get("courseId");

                        boolean deleteCourseResult = courseService.deleteCourse(deleteCourseId);
                        response = deleteCourseResult ?
                                Response.success("删除课程成功") :
                                Response.error("删除课程失败");
                        break;

                    // 添加教学班（管理员功能）
                    case "addTeachingClass":
                        // 兼容两种前端格式：
                        // 1) data.teachingClass = { ... }
                        // 2) data 直接包含教学班字段
                        Map<String, Object> teachingClassData = null;
                        if (request.getData() != null) {
                            Object tcObj = request.getData().get("teachingClass");
                            if (tcObj instanceof Map) {
                                teachingClassData = (Map<String, Object>) tcObj;
                            } else {
                                teachingClassData = request.getData();
                            }
                        }

                        // 如果 data 为 null，则直接返回错误，避免在 createTeachingClassFromMap 中触发 NPE
                        if (teachingClassData == null) {
                            response = Response.error("请求参数不完整: teachingClass 数据缺失");
                            break;
                        }

                        TeachingClass newTeachingClass = createTeachingClassFromMap(teachingClassData);
                        // 规范并校验 schedule 字段，确保写入数据库的为合法 JSON 字符串
                        if (newTeachingClass != null && newTeachingClass.getSchedule() != null) {
                            String normalized = normalizeScheduleForStorage(newTeachingClass.getSchedule());
                            if (normalized == null) {
                                response = Response.error("schedule 字段格式不正确，应为合法的 JSON，例如 {\"周三\": \"1-2节\"}");
                                break;
                            }
                            newTeachingClass.setSchedule(normalized);
                        }
                        // 保证写入数据库时 selectedCount 和 capacity 不为 null（数据库有 NOT NULL 约束）
                        if (newTeachingClass.getSelectedCount() == null) newTeachingClass.setSelectedCount(0);
                        if (newTeachingClass.getCapacity() == null) newTeachingClass.setCapacity(0);
                        boolean addTeachingClassResult = teachingClassService.addTeachingClass(newTeachingClass);
                         response = addTeachingClassResult ?
                                 Response.success("添加教学班成功") :
                                 Response.error("添加教学班失败");
                         break;

                    // 更新教学班（部分更新）
                    case "updateTeachingClass":
                        String updateUuid = null;
                        if (request.getData() != null && request.getData().containsKey("uuid")) {
                            updateUuid = (String) request.getData().get("uuid");
                        }

                        // 兼容两种前端格式：
                        // 1) data.updates = {...}（原有）
                        // 2) data 直接包含要更新的字段（如示例）
                        Map<String, Object> updates = null;
                        if (request.getData() != null) {
                            Object u = request.getData().get("updates");
                            if (u instanceof Map) {
                                updates = (Map<String, Object>) u;
                            } else {
                                updates = request.getData();
                            }
                        }

                        if (updateUuid == null || updateUuid.trim().isEmpty()) {
                            response = Response.error("缺少参数: uuid");
                            break;
                        }

                        // 获取现有教学班信息
                        TeachingClass existingTeachingClass = teachingClassService.findByUuid(updateUuid);
                        if (existingTeachingClass == null) {
                            response = Response.error("教学班不存在");
                            break;
                        }

                        // 应用更新 - 检查并更新所有可能的字段（updates 可能是 request.data 本身）
                        if (updates != null && updates.containsKey("courseId")) {
                            existingTeachingClass.setCourseId((String) updates.get("courseId"));
                        }
                        if (updates != null && updates.containsKey("teacherName")) {
                            existingTeachingClass.setTeacherName((String) updates.get("teacherName"));
                        }
                        if (updates != null && updates.containsKey("schedule")) {
                            Object schedObj = updates.get("schedule");
                            if (schedObj != null) {
                                // 如果前端直接传了 Map 或 List，则直接传递原始对象给 normalize 函数
                                String normalized;
                                if (schedObj instanceof Map || schedObj instanceof java.util.List) {
                                    normalized = normalizeScheduleForStorage(schedObj);
                                } else {
                                    String schedStr = String.valueOf(schedObj);
                                    normalized = normalizeScheduleForStorage(schedStr);
                                }
                                 if (normalized == null) {
                                     response = Response.error("schedule 字段格式不正确，应为合法的 JSON，例如 {\"周三\": \"1-2节\"}");
                                     break;
                                 }
                                 existingTeachingClass.setSchedule(normalized);
                             }
                         }
                        if (updates != null && updates.containsKey("place")) {
                            existingTeachingClass.setPlace((String) updates.get("place"));
                        }
                        if (updates != null && updates.containsKey("capacity")) {
                            Object capObj = updates.get("capacity");
                            if (capObj instanceof Number) {
                                existingTeachingClass.setCapacity(((Number) capObj).intValue());
                            } else if (capObj instanceof String) {
                                try { existingTeachingClass.setCapacity(Integer.parseInt((String) capObj)); } catch (NumberFormatException ignored) {}
                            }
                        }
                        if (updates != null && updates.containsKey("selectedCount")) {
                            Object scObj = updates.get("selectedCount");
                            if (scObj instanceof Number) {
                                existingTeachingClass.setSelectedCount(((Number) scObj).intValue());
                            } else if (scObj instanceof String) {
                                try { existingTeachingClass.setSelectedCount(Integer.parseInt((String) scObj)); } catch (NumberFormatException ignored) {}
                            }
                        }

                        // 保存更新
                        boolean updateResult1 = teachingClassService.updateTeachingClass(existingTeachingClass);
                        response = updateResult1 ?
                                Response.success("更新教学班成功") :
                                Response.error("更新教学班失败");
                        break;

                    // 删除教学班（管理员功能）
                    case "deleteTeachingClass":
                        // 兼容前端可能使用的字段名：teachingClassUuid 或 uuid
                        String deleteTeachingClassUuid = null;
                        if (request.getData() != null) {
                            if (request.getData().containsKey("teachingClassUuid")) {
                                deleteTeachingClassUuid = (String) request.getData().get("teachingClassUuid");
                            } else if (request.getData().containsKey("uuid")) {
                                deleteTeachingClassUuid = (String) request.getData().get("uuid");
                            }
                        }

                        if (deleteTeachingClassUuid == null || deleteTeachingClassUuid.trim().isEmpty()) {
                            response = Response.error("缺少参数: teachingClassUuid 或 uuid");
                            break;
                        }

                        boolean deleteTeachingClassResult = teachingClassService.deleteTeachingClass(deleteTeachingClassUuid);
                        response = deleteTeachingClassResult ?
                                Response.success("删除教学班成功") :
                                Response.error("删除教学班失败");
                         break;

                    case "getFinanceCard":
                        Object cardNumberObj = request.getData().get("cardNumber");
                        if (cardNumberObj == null) {
                            response = Response.error("缺少参数: cardNumber");
                            break;
                        }
                        try {
                            Integer cardNumber1 = ((Double) cardNumberObj).intValue();
                            FinanceCard financeCard = financeService.getFinanceCard(cardNumber1);
                            if (financeCard != null) {
                                response = Response.success("获取一卡通信息成功", financeCard);
                            } else {
                                response = Response.error("未找到一卡通信息");
                            }
                        } catch (ClassCastException e) {
                            response = Response.error("cardNumber参数类型错误");
                        } catch (Exception e) {
                            response = Response.error("获取一卡通信息失败: " + e.getMessage());
                        }
                        break;

                    case "rechargeFinanceCard":
                        Integer rechargeCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        Integer amount = ((Double) request.getData().get("amount")).intValue();
                        String description = (String) request.getData().get("description");

                        try {
                            boolean rechargeResult = financeService.rechargeFinanceCard(rechargeCardNumber, amount, description);
                            response = rechargeResult ?
                                    Response.success("充值成功") :
                                    Response.error("充值失败");
                        } catch (Exception e) {
                            response = Response.error("充值失败: " + e.getMessage());
                        }
                        break;

                    case "reportLoss":
                        // 用户挂失自己的卡
                        Map<String, Object> reportLossData = request.getData();
                        Integer reportCardNumber = ((Double) reportLossData.get("cardNumber")).intValue();

                        response = financeService.reportLoss(reportCardNumber);
                        break;

                    case "cancelReportLoss":
                        // 管理员解除挂失
                        Map<String, Object> cancelReportData = request.getData();
                        Integer targetCardNumber = ((Double) cancelReportData.get("targetCardNumber")).intValue();

                        response = financeService.cancelReportLoss(targetCardNumber);
                        break;

                    case "findAllLostCards":
                        // 管理员查询所有挂失的一卡通账号
                        response = financeService.findAllLostCards();
                        break;

                    case "getTransactions":
                        Integer transactionCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        String transactionType = (String) request.getData().get("type");

                        List<CardTransaction> transactions = financeService.getTransactions(transactionCardNumber, transactionType);
                        // 手动转换为 DTO，避免直接序列化 LocalDateTime
                        List<Map<String, Object>> txDtoList = new ArrayList<>();
                        if (transactions != null) {
                            for (CardTransaction ct : transactions) {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("transactionId", ct.getUuid() == null ? null : ct.getUuid().toString());
                                m.put("cardNumber", ct.getCardNumber());
                                m.put("amount", ct.getAmount());
                                m.put("type", ct.getType());
                                m.put("description", ct.getDescription());
                                // 继续输出 ISO 字符串（客户端已兼容）
                                m.put("timestamp", ct.getTime() == null ? null : ct.getTime().toString());
                                txDtoList.add(m);
                            }
                        }
                        response = Response.success("获取交易记录成功", txDtoList);
                        break;

                    case "getAllItems":
                        List<StoreItem> items = storeService.getAllItems();
                        response = Response.success("获取商品列表成功", items);
                        break;

                    case "searchItems":
                        String keyword = (String) request.getData().get("keyword");
                        List<StoreItem> searchResults = storeService.searchItems(keyword);
                        response = Response.success("搜索完成", searchResults);
                        break;

                    case "getItemById":
                        String itemIdStr = (String) request.getData().get("itemId");
                        try {
                            UUID itemId = UUID.fromString(itemIdStr);
                            StoreItem item = storeService.getItemById(itemId);
                            response = item != null ? Response.success("获取商品成功", item) : Response.error("商品不存在");
                        } catch (IllegalArgumentException e) {
                            response = Response.error("商品ID格式不正确");
                        }
                        break;

                    case "addItem":
                        // 管理员功能：添加商品
                        Map<String, Object> itemData = (Map<String, Object>) request.getData().get("item");
                        StoreItem newItem = createStoreItemFromMap(itemData);
                        boolean addItemResult = storeService.addItem(newItem);
                        response = addItemResult ? Response.success("添加商品成功") : Response.error("添加商品失败");
                        break;

                    case "updateItem":
                        // 管理员功能：更新商品
                        Map<String, Object> updateItemData = (Map<String, Object>) request.getData().get("item");
                        if (updateItemData == null || updateItemData.get("uuid") == null) {
                            response = Response.error("更新商品需要提供uuid");
                            break;
                        }
                        StoreItem updateItem = createStoreItemFromMap(updateItemData);
                        boolean updateItemResult = storeService.updateItem(updateItem);
                        response = updateItemResult ? Response.success("更新商品成功") : Response.error("更新商品失败");
                        break;

                    case "deleteItem":
                        // 管理员功能：删除商品
                        String deleteItemIdStr = (String) request.getData().get("itemId");
                        try {
                            UUID deleteItemId = UUID.fromString(deleteItemIdStr);
                            boolean deleteItemResult = storeService.deleteItem(deleteItemId);
                            response = deleteItemResult ? Response.success("删除商品成功") : Response.error("删除商品失败");
                        } catch (IllegalArgumentException e) {
                            response = Response.error("商品ID格式不正确");
                        }
                        break;

                    // 商品类别相关功能
                    case "getItemsByCategory":
                        String category = (String) request.getData().get("category");
                        List<StoreItem> categoryItems = storeService.getItemsByCategory(category);
                        response = Response.success("获取类别商品成功", categoryItems);
                        break;

//                    case "getAllCategories":
//                        List<String> categories = storeService.getAllCategories();
//                        response = Response.success("获取所有类别成功", categories);
//                        break;

                    case "searchItemsByCategory":
                        String searchCategory = (String) request.getData().get("category");
                        String searchKeyword = (String) request.getData().get("keyword");
                        List<StoreItem> categorySearchResults = storeService.searchItemsByCategoryAndKeyword(searchCategory, searchKeyword);
                        response = Response.success("按类别搜索完成", categorySearchResults);
                        break;

                    case "createOrder":
                        Map<String, Object> orderData = request.getData();
                        Integer orderCardNumber =  Integer.valueOf((String) orderData.get("cardNumber"));
                        String orderRemark = (String) orderData.get("remark");

                        // 解析订单商品项
                        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) orderData.get("items");
                        List<StoreOrderItem> orderItems = new ArrayList<>();
                        Integer totalAmount = 0;
                        boolean orderItemError = false;

                        for (Map<String, Object> itemData1 : itemsData) {
                            String itemIdStr1 = (String) itemData1.get("itemId");
                            Integer itemAmount = ((Double) itemData1.get("amount")).intValue();

                            try {
                                UUID itemId = UUID.fromString(itemIdStr1);

                                // 获取商品信息以获取价格
                                StoreItem item = storeService.getItemById(itemId);
                                if (item == null) {
                                    response = Response.error("商品不存在: " + itemIdStr1);
                                    orderItemError = true;
                                    break;
                                }

                                StoreOrderItem orderItem = new StoreOrderItem(null, itemId, item.getPrice(), itemAmount);
                                orderItems.add(orderItem);
                                totalAmount += item.getPrice() * itemAmount;

                            } catch (IllegalArgumentException e) {
                                response = Response.error("商品ID格式不正确: " + itemIdStr1);
                                orderItemError = true;
                                break;
                            }
                        }
                        if (orderItemError) break; // 直接结束switch
                        try {
                            StoreOrder order = new StoreOrder(orderCardNumber, totalAmount, orderRemark, orderItems);
                            StoreOrder createdOrder = storeService.createOrder(order);
                            response = Response.success("创建订单成功", createdOrder);
                        } catch (Exception e) {
                            response = Response.error("创建订单失败: " + e.getMessage());
                        }
                        break;

                    case "payOrder":
                        String payOrderIdStr = (String) request.getData().get("orderId");
                        try {
                            UUID orderId = UUID.fromString(payOrderIdStr);
                            boolean payResult = storeService.payOrder(orderId);
                            response = payResult ? Response.success("支付成功") : Response.error("支付失败");
                        } catch (IllegalArgumentException e) {
                            response = Response.error("订单ID格式不正确");
                        } catch (Exception e) {
                            response = Response.error("支付失败: " + e.getMessage());
                        }
                        break;

                    case "cancelOrder":
                        String cancelOrderIdStr = (String) request.getData().get("orderId");
                        try {
                            UUID orderId = UUID.fromString(cancelOrderIdStr);
                            boolean cancelResult = storeService.cancelOrder(orderId);
                            response = cancelResult ? Response.success("取消订单成功") : Response.error("取消订单失败");
                        } catch (IllegalArgumentException e) {
                            response = Response.error("订单ID格式不正确");
                        } catch (Exception e) {
                            response = Response.error("取消订单失败: " + e.getMessage());
                        }
                        break;

                    case "getUserOrders":
                        Integer userCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        List<StoreOrder> userOrders = storeService.getUserOrders(userCardNumber);
                        response = Response.success("获取用户订单成功", userOrders);
                        break;

                    case "getAllOrders":
                        // 管理员功能：获取所有订单
                        List<StoreOrder> allOrders = storeService.getAllOrders();
                        response = Response.success("获取所有订单成功", allOrders);
                        break;

                    case "getOrder":
                        String getOrderIdStr = (String) request.getData().get("orderId");
                        try {
                            UUID orderId = UUID.fromString(getOrderIdStr);
                            StoreOrder order = storeService.getOrderById(orderId);
                            response = (order != null) ? Response.success("获取订单成功", order) : Response.error("订单不存在");
                        } catch (IllegalArgumentException e) {
                            response = Response.error("订单ID格式不正确");
                        }
                        break;

                    case "getSalesStats":
                        // 管理员功能：获取销售统计
                        List<StoreMapper.SalesStats> salesStats = storeService.getSalesStatistics();
                        response = Response.success("获取销售统计成功", salesStats);
                        break;

                    case "getTodaySalesStats":
                        // 管理员功能：获取今日销售统计
                        List<StoreMapper.SalesStats> todaySalesStats = storeService.getTodaySalesStatistics();
                        response = Response.success("获取今日销售统计成功", todaySalesStats);
                        break;

                    case "getTodaySales":
                        // 管理员功能：获取今日销售总额
                        Integer todaySales = storeService.getTodaySalesRevenue();
                        response = Response.success("获取今日销售总额成功", todaySales);
                        break;

                    case "getSales":
                        // 管理员功能：获取销售总额
                        Integer Sales = storeService.getSalesRevenue();
                        response = Response.success("获取销售总额成功", Sales);
                        break;

                    case "refundOrder":
                        // 管理员功能：订单退款
                        Map<String, Object> refundData = request.getData();
                        String refundOrderIdStr = (String) refundData.get("orderId");
                        String refundReason = (String) refundData.get("reason");

                        try {
                            UUID orderId = UUID.fromString(refundOrderIdStr);
                            boolean refundResult = storeService.refundOrder(orderId, refundReason);
                            response = refundResult ?
                                    Response.success("退款成功") :
                                    Response.error("退款失败");
                        } catch (IllegalArgumentException e) {
                            response = Response.error("订单ID格式不正确");
                        } catch (Exception e) {
                            response = Response.error("退款失败: " + e.getMessage());
                        }
                        break;

                    // 🔍 搜索书籍（通过书名）
                    case "searchBooks":
                        String searchBookText = (String) request.getData().get("searchText");
                        String categoryStr = (String) request.getData().get("category"); // 前端传类别字符串，比如 "SCIENCE" 或 null/空表示全部
                        Category categorybook = null;
                        if (categoryStr != null) {
                            categorybook = Category.valueOf(categoryStr); // 将字符串转为枚举
                        }
                        try {
                            List<Book> books = bookService.searchBooks(searchBookText, categorybook);
                            response = Response.success("搜索完成", books);
                        } catch (Exception e) {
                            response = Response.error(500, "搜索过程中发生错误: " + e.getMessage());
                        }
                        break;

                    // 📖 获取个人借阅记录（通过 userId）
                    case "getOwnRecords": {
                        Integer userId = ((Double) request.getData().get("userId")).intValue();
                        if (userId == 0) {
                            response = Response.error("缺少 userId 参数");
                            break;
                        }
                        try {
                            List<BookRecord> records = bookService.userRecords(userId);
                            response = Response.success("查询成功", records);
                        } catch (Exception e) {
                            response = Response.error(500, "查询过程中发生错误: " + e.getMessage());
                        }
                        break;
                    }

                    // 🔄 续借图书
                    case "renewBook": {
                        String uuid = (String) request.getData().get("uuid");
                        if (uuid == null) {
                            response = Response.error("缺少图书 uuid 参数");
                            break;
                        }
                        try {
                            boolean result = bookService.renewBook(uuid);
                            response = result ? Response.success("续借成功") : Response.error("续借失败");
                        } catch (Exception e) {
                            response = Response.error(500, "续借过程中发生错误: " + e.getMessage());
                        }
                        break;
                    }

                    // ✏ 更新书籍信息
                    case "updateBook": {
                        Map<String, Object> bookData = (Map<String, Object>) request.getData().get("book");
                        Book bookUpdate = createBookFromMap(bookData);
                        boolean result = bookService.updateBook(bookUpdate);
                        response = result ? Response.success("更新成功") : Response.error("更新失败");
                        break;
                    }

                    // ❌ 删除书籍（根据 ISBN）
                    case "deleteBook": {
                        String isbn = (String) request.getData().get("isbn");
                        if (isbn == null) {
                            response = Response.error("缺少 ISBN 参数");
                            break;
                        }
                        boolean result = bookService.deleteBook(isbn);
                        response = result ? Response.success("删除成功") : Response.error("删除失败");
                        break;
                    }

                    // ➕ 添加书籍
                    case "addBook": {
                        Map<String, Object> bookData = (Map<String, Object>) request.getData().get("book");
                        Book newBook = createBookFromMap(bookData);
                        boolean result = bookService.addBook(newBook);
                        response = result ? Response.success("添加成功", newBook.getIsbn()) : Response.error("添加失败");
                        break;
                    }

                    // 📚 借书
                    case "borrowBook": {
                        String isbn = (String) request.getData().get("uuid");
                        Integer userId = ((Double) request.getData().get("userId")).intValue();
                        if (isbn == null || userId == 0) {
                            response = Response.error("缺少 uuid 或 userId 参数");
                            break;
                        }
                        boolean result = bookService.borrowBook(userId, isbn);
                        response = result ? Response.success("借书成功") : Response.error("借书失败");
                        break;
                    }

                    // 🔙 还书
                    case "returnBook": {
                        String uuid = (String) request.getData().get("uuid");
                        if (uuid == null) {
                            response = Response.error("缺少 uuid 参数");
                            break;
                        }
                        boolean result = bookService.returnBook(uuid);
                        response = result ? Response.success("还书成功") : Response.error("还书失败");
                        break;
                    }

                    // ➕ 添加书籍实体
                    case "addBookItem": {
                        Map<String, Object> bookitemData = (Map<String, Object>) request.getData().get("bookItem");
                        BookItem newbookItem = createBookItemFromMap(bookitemData); // 需要自己写的方法，将 Map 转成 BookItem
                        boolean result = bookService.addBookItem(newbookItem);
                        response = result ? Response.success("添加成功", newbookItem.getUuid()) : Response.error("添加失败");
                        break;
                    }

                    // ❌ 删除书籍实体
                    case "deleteBookItem": {
                        String uuid = (String) request.getData().get("uuid");
                        if (uuid == null) {
                            response = Response.error("缺少 uuid 参数");
                            break;
                        }
                        boolean result = bookService.deleteBookItem(uuid);
                        response = result ? Response.success("删除成功") : Response.error("删除失败");
                        break;
                    }

                    // ✏ 更新书籍实体
                    case "updateBookItem": {
                        Map<String, Object> bookitemData = (Map<String, Object>) request.getData().get("bookItem");
                        BookItem itemUpdate = createBookItemFromMap(bookitemData);
                        boolean result = bookService.updateBookItem(itemUpdate);
                        response = result ? Response.success("更新成功") : Response.error("更新失败");
                        break;
                    }

                    // 🔍 查询书籍实体（根据 UUID）
                    case "findBookItem": {
                        String uuid = (String) request.getData().get("uuid");
                        if (uuid == null) {
                            response = Response.error("缺少 uuid 参数");
                            break;
                        }
                        try {
                            BookItem item = bookService.getBookItemByUuid(uuid);
                            response = item != null ? Response.success("查询成功", item) : Response.error("未找到对应书籍实体");
                        } catch (Exception e) {
                            response = Response.error(500, "查询过程中发生错误: " + e.getMessage());
                        }
                        break;
                    }

                    // 🔍 根据 ISBN 搜索书籍实体
                    case "searchBookItems": {
                        String isbn = (String) request.getData().get("isbn");
                        if (isbn == null) {
                            response = Response.error("缺少 ISBN 参数");
                            break;
                        }
                        try {
                            List<BookItem> itembooks = bookService.retrieveBookItems(isbn);
                            response = Response.success("查询成功", itembooks);
                        } catch (Exception e) {
                            response = Response.error(500, "查询过程中发生错误: " + e.getMessage());
                        }
                        break;
                    }

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
        if (data.containsKey("enrollment")) student.setEnrollment(new Date(((Double) data.get("enrollment")).longValue()));
        if (data.containsKey("birth")) student.setBirth(new Date(((Double) data.get("birth")).longValue()));
        if (data.containsKey("birthPlace")) student.setBirthPlace((String) data.get("birthPlace"));
        if (data.containsKey("politicalStat")) student.setPoliticalStat(PoliticalStatus.valueOf((String) data.get("politicalStat")));
        if (data.containsKey("gender")) student.setGender(Gender.valueOf((String) data.get("gender")));
        if (data.containsKey("name")) student.setName((String) data.get("name"));

        return student;
    }

    // 添加辅助方法，用于从Map创建Course对象
    private Course createCourseFromMap(Map<String, Object> data) {
        Course course = new Course();

        if (data.containsKey("courseId")) course.setCourseId((String) data.get("courseId"));
        if (data.containsKey("courseName")) course.setCourseName((String) data.get("courseName"));
        if (data.containsKey("school")) course.setSchool((String) data.get("school"));
        if (data.containsKey("credit")) course.setCredit(((Double) data.get("credit")).floatValue());

        return course;
    }

    // 添加辅助方法，用于从Map创建TeachingClass对象
    private TeachingClass createTeachingClassFromMap(Map<String, Object> data) {
        if (data == null) return null;

        TeachingClass teachingClass = new TeachingClass();

        if (data.containsKey("uuid") && data.get("uuid") != null) teachingClass.setUuid((String) data.get("uuid"));
        if (data.containsKey("courseId") && data.get("courseId") != null) teachingClass.setCourseId((String) data.get("courseId"));
        // 修正前端字段名 teacherName
        if (data.containsKey("teacherName") && data.get("teacherName") != null) teachingClass.setTeacherName((String) data.get("teacherName"));
        // schedule 可能是字符串也可能是 Map（客户端直接传对象）
        if (data.containsKey("schedule") && data.get("schedule") != null) {
            Object sched = data.get("schedule");
            if (sched instanceof Map) {
                // 直接将对象序列化为 JSON 字符串保存到 model
                teachingClass.setSchedule(gson.toJson(sched));
            } else {
                teachingClass.setSchedule(String.valueOf(sched));
            }
        }
        if (data.containsKey("place") && data.get("place") != null) teachingClass.setPlace((String) data.get("place"));

        if (data.containsKey("capacity") && data.get("capacity") != null) {
            Object cap = data.get("capacity");
            if (cap instanceof Number) {
                teachingClass.setCapacity(((Number) cap).intValue());
            } else if (cap instanceof String) {
                try { teachingClass.setCapacity(Integer.parseInt((String) cap)); } catch (NumberFormatException ignored) {}
            }
        }

        if (data.containsKey("selectedCount") && data.get("selectedCount") != null) {
            Object sc = data.get("selectedCount");
            if (sc instanceof Number) {
                teachingClass.setSelectedCount(((Number) sc).intValue());
            } else if (sc instanceof String) {
                try { teachingClass.setSelectedCount(Integer.parseInt((String) sc)); } catch (NumberFormatException ignored) {}
            }
        }

        // 如果未指定 selectedCount，默认设为0，避免数据库 NOT NULL 约束错误
        if (teachingClass.getSelectedCount() == null) {
            teachingClass.setSelectedCount(0);
        }
        // 若未指定 capacity，也设为0以避免后续比较 NPE
        if (teachingClass.getCapacity() == null) {
            teachingClass.setCapacity(0);
        }

        return teachingClass;
    }

    // 添加辅助方法，用于从Map创建StoreItem对象
    private StoreItem createStoreItemFromMap(Map<String, Object> data) {
        StoreItem item = new StoreItem();

        if (data.containsKey("uuid")) {
            item.setUuid(UUID.fromString((String) data.get("uuid")));
        } else {
            item.setUuid(UUID.randomUUID());
        }

        if (data.containsKey("itemName")) item.setItemName((String) data.get("itemName"));
        if (data.containsKey("price")) item.setPrice(((Double) data.get("price")).intValue());
        if (data.containsKey("pictureLink")) item.setPictureLink((String) data.get("pictureLink"));
        if (data.containsKey("stock")) item.setStock(((Double) data.get("stock")).intValue());
        if (data.containsKey("salesVolume")) item.setSalesVolume(((Double) data.get("salesVolume")).intValue());
        if (data.containsKey("description")) item.setDescription((String) data.get("description"));
        if (data.containsKey("category")) item.setCategory((String) data.get("category"));
        if (data.containsKey("barcode")) item.setBarcode((String) data.get("barcode"));

        return item;
    }

    // 添加到 ClientHandler 或者单独写一个工具类
    private Book createBookFromMap(Map<String, Object> data) {
        Book book = new Book();

        if (data.containsKey("name")) {
            book.setName((String) data.get("name"));
        }
        if (data.containsKey("isbn")) {
            book.setIsbn((String) data.get("isbn"));
        }
        if (data.containsKey("author")) {
            book.setAuthor((String) data.get("author"));
        }
        if (data.containsKey("publisher")) {
            book.setPublisher((String) data.get("publisher"));
        }
        if (data.containsKey("publishDate")) {
            // 前端传的可能是 "2025-09-12" 这样的字符串
            String dateStr = (String) data.get("publishDate");
            if (dateStr != null && !dateStr.isEmpty()) {
                book.setPublishDate(LocalDate.parse(dateStr));
            }
        }
        if (data.containsKey("description")) {
            book.setDescription((String) data.get("description"));
        }
        if (data.containsKey("inventory")) {
            // JSON 里数字会被 GSON 转成 Double，这里要转 int
            Object invObj = data.get("inventory");
            if (invObj instanceof Number) {
                book.setInventory(((Number) invObj).intValue());
            }
        }
        if (data.containsKey("category")) {
            // 前端传过来的值是枚举名，比如 "SCIENCE"
            String categoryStr = (String) data.get("category");
            if (categoryStr != null) {
                try {
                    book.setCategory(Category.valueOf(categoryStr));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("无效的类别: " + categoryStr);
                }
            }
        }

        return book;
    }
    public BookItem createBookItemFromMap(Map<String, Object> map) {
        if (map == null) return null;

        BookItem bookItem = new BookItem();

        // UUID
        Object uuidObj = map.get("uuid");
        if (uuidObj != null) {
            bookItem.setUuid(uuidObj.toString());
        }

        // ISBN
        Object isbnObj = map.get("isbn");
        if (isbnObj != null) {
            bookItem.setIsbn(isbnObj.toString());
        }

        // 馆藏位置
        Object placeObj = map.get("place");
        if (placeObj != null) {
            bookItem.setPlace(placeObj.toString());
        }

        // 书籍状态（字符串转枚举）
        Object statusObj = map.get("bookStatus");
        if (statusObj != null) {
            String statusStr = statusObj.toString().toUpperCase(); // 支持传 "INLIBRARY" 或 "lend"
            try {
                BookStatus status = BookStatus.valueOf(statusStr);
                bookItem.setBookStatus(status);
            } catch (IllegalArgumentException e) {
                // 默认状态为 INLIBRARY，如果前端传错值
                bookItem.setBookStatus(BookStatus.INLIBRARY);
            }
        } else {
            bookItem.setBookStatus(BookStatus.INLIBRARY);
        }

        return bookItem;
    }

    // 用于表示一个时间段
    private static class TimeRange {
        LocalTime start;
        LocalTime end;
        TimeRange(LocalTime s, LocalTime e) { start = s; end = e; }
    }

    // 将 schedule JSON 解析为 Map<day, List<TimeRange>>，兼容单个或逗号分隔的多个时间段
    private Map<String, List<TimeRange>> parseSchedule(String scheduleJson) {
        Map<String, List<TimeRange>> map = new HashMap<>();
        if (scheduleJson == null || scheduleJson.trim().isEmpty()) return map;
        try {
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
            Map<String, String> raw = gson.fromJson(scheduleJson, mapType);
            if (raw == null) return map;
            for (Map.Entry<String, String> e : raw.entrySet()) {
                String day = e.getKey();
                String val = e.getValue();
                if (val == null) continue;
                // 支持逗号或分号分隔，允许空白符
                String[] parts = val.split("[,;]\\s*");
                List<TimeRange> ranges = new ArrayList<>();

                // 节次与时间的映射（根据学校实际时间可调整）
                String[] periodStart = new String[]{
                        "", // 0 占位
                        "08:00", "08:50", "10:00", "10:50",
                        "14:00", "14:50", "15:50", "16:40",
                        "19:00", "19:50", "20:10", "20:55"
                };
                String[] periodEnd = new String[]{
                        "",
                        "08:45", "09:35", "10:45", "11:30",
                        "14:45", "15:35", "16:35", "17:25",
                        "19:45", "20:35", "20:50", "21:40"
                };

                for (String p : parts) {
                    // 规范化中文标点与空白
                    String rawPart = p.replace('：', ':').replace('－', '-').replace('—', '-').replace('–', '-').trim();
                    if (rawPart.isEmpty()) continue;

                    // 如果包含 ':'，仍按时间区间解析（如 08:00-09:40）
                    if (rawPart.contains(":")) {
                        String[] se = rawPart.split("-");
                        if (se.length != 2) continue;
                        String startStr = se[0].trim();
                        String endStr = se[1].trim();
                        // 尝试多种时间格式解析（H:mm / HH:mm / H:mm:ss）
                        java.time.LocalTime s = null, t = null;
                        java.time.format.DateTimeFormatter[] fmts = new java.time.format.DateTimeFormatter[] {
                                java.time.format.DateTimeFormatter.ofPattern("H:mm"),
                                java.time.format.DateTimeFormatter.ofPattern("HH:mm"),
                                java.time.format.DateTimeFormatter.ofPattern("H:mm:ss"),
                                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                        };
                        for (java.time.format.DateTimeFormatter fmt : fmts) {
                            if (s == null) {
                                try { s = java.time.LocalTime.parse(startStr, fmt); } catch (Exception ignored) {}
                            }
                            if (t == null) {
                                try { t = java.time.LocalTime.parse(endStr, fmt); } catch (Exception ignored) {}
                            }
                            if (s != null && t != null) break;
                        }
                        if (s != null && t != null) ranges.add(new TimeRange(s, t));
                        continue;
                    }

                    // 识别节次格式，例如 "1-2节"、"1-2"、"3节" 或 "3"
                    java.util.regex.Pattern rangePat = java.util.regex.Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*节?$");
                    java.util.regex.Matcher m = rangePat.matcher(rawPart);
                    if (m.find()) {
                        try {
                            int a = Integer.parseInt(m.group(1));
                            int b = Integer.parseInt(m.group(2));
                            if (a < 1) a = 1;
                            if (b < 1) b = 1;
                            if (a >= periodStart.length) a = periodStart.length - 1;
                            if (b >= periodEnd.length) b = periodEnd.length - 1;
                            if (a > b) { int tmp = a; a = b; b = tmp; }
                            String ss = periodStart[a];
                            String ee = periodEnd[b];
                            if (ss != null && ee != null && !ss.isEmpty() && !ee.isEmpty()) {
                                ranges.add(new TimeRange(java.time.LocalTime.parse(ss), java.time.LocalTime.parse(ee)));
                                continue;
                            }
                        } catch (Exception ignored) {}
                    }
                    java.util.regex.Pattern singlePat = java.util.regex.Pattern.compile("^(\\d+)\\s*节?$");
                    m = singlePat.matcher(rawPart);
                    if (m.find()) {
                        try {
                            int pnum = Integer.parseInt(m.group(1));
                            if (pnum < 1) pnum = 1;
                            if (pnum >= periodStart.length) pnum = periodStart.length - 1;
                            String ss = periodStart[pnum];
                            String ee = periodEnd[pnum];
                            if (ss != null && ee != null && !ss.isEmpty() && !ee.isEmpty()) {
                                ranges.add(new TimeRange(java.time.LocalTime.parse(ss), java.time.LocalTime.parse(ee)));
                                continue;
                            }
                        } catch (Exception ignored) {}
                    }

                    // 若都无法识别，尝试原有的用 '-' 分割解析（兼容无冒号但有连字符的时间）
                    String[] se = rawPart.split("-");
                    if (se.length == 2) {
                        String startStr = se[0].trim();
                        String endStr = se[1].trim();
                        java.time.LocalTime s = null, t = null;
                        java.time.format.DateTimeFormatter[] fmts = new java.time.format.DateTimeFormatter[] {
                                java.time.format.DateTimeFormatter.ofPattern("H:mm"),
                                java.time.format.DateTimeFormatter.ofPattern("HH:mm"),
                                java.time.format.DateTimeFormatter.ofPattern("H:mm:ss"),
                                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                        };
                        for (java.time.format.DateTimeFormatter fmt : fmts) {
                            if (s == null) {
                                try { s = java.time.LocalTime.parse(startStr, fmt); } catch (Exception ignored) {}
                            }
                            if (t == null) {
                                try { t = java.time.LocalTime.parse(endStr, fmt); } catch (Exception ignored) {}
                            }
                            if (s != null && t != null) break;
                        }
                        if (s != null && t != null) ranges.add(new TimeRange(s, t));
                    }
                }
                if (!ranges.isEmpty()) map.put(day, ranges);
            }
        } catch (Exception ex) {
            // ignore parse errors
        }
        return map;
    }

    // 检查两个 schedule 是否有冲突（同一周日有重叠时间段即视为冲突）
    private boolean schedulesConflict(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        Map<String, List<TimeRange>> m1 = parseSchedule(s1);
        Map<String, List<TimeRange>> m2 = parseSchedule(s2);
        for (String day : m1.keySet()) {
            if (!m2.containsKey(day)) continue;
            List<TimeRange> r1 = m1.get(day);
            List<TimeRange> r2 = m2.get(day);
            for (TimeRange a : r1) for (TimeRange b : r2) {
                if (a.start.isBefore(b.end) && b.start.isBefore(a.end)) return true;
            }
        }
        return false;
    }

    // 将前端传来的 schedule 字符串规范化为合法的 JSON 字符串以写入数据库
    // 返回规范化的 JSON（如: {"周三":"1-2节"} 或 {"周六":"1-2节,6-7节"}），失败返回 null
    private String normalizeScheduleForStorage(Object raw) {
        if (raw == null) return null;
        // 已经是 Map 对象（客户端可能直接传了对象）
        if (raw instanceof Map) {
            try {
                Map<?, ?> inMap = (Map<?, ?>) raw;
                Map<String, String> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : inMap.entrySet()) {
                    if (e.getKey() == null) continue;
                    String day = String.valueOf(e.getKey()).trim();
                    if (day.isEmpty()) continue;
                    Object v = e.getValue();
                    if (v == null) continue;
                    // 值可能是字符串，也可能是 List
                    if (v instanceof java.util.List) {
                        List<?> lst = (List<?>) v;
                        List<String> parts = new ArrayList<>();
                        for (Object it : lst) {
                            if (it == null) continue;
                            String s = String.valueOf(it).trim();
                            if (!s.isEmpty() && !parts.contains(s)) parts.add(s);
                        }
                        if (!parts.isEmpty()) out.put(day, String.join(",", parts));
                    } else {
                        String s = String.valueOf(v).trim();
                        if (!s.isEmpty()) out.put(day, s);
                    }
                }
                if (out.isEmpty()) return null;
                return gson.toJson(out);
            } catch (Exception e) {
                return null;
            }
        }

        String s = String.valueOf(raw).trim();
        // 替换常见中文全角标点
        s = s.replace('：', ':')
             .replace('，', ',')
             .replace('；', ';')
             .replace('（', '(').replace('）', ')')
             .replace('“', '"').replace('”', '"')
             .replace('‘', '\'').replace('’', '\'');

        // 如果字符串里的对象使用了未加引号的 key（例如 {周六: "1-2节"}），为其添加引号，方便 Gson 解析
        try {
            if (s.startsWith("{") && s.contains(":")) {
                // 使用 Pattern/Matcher 代替复杂的字符串字面量正则，避免转义错误
                java.util.regex.Pattern keyPattern = java.util.regex.Pattern.compile("(\\{|,)\\s*([^\"'\\[\\]{}\\.,:]+?)\\s*:");
                java.util.regex.Matcher keyMatcher = keyPattern.matcher(s);
                s = keyMatcher.replaceAll("$1\"$2\":");
            }
        } catch (Exception ex) {
            // 忽略替换失败，后续 Gson 解析会处理或返回 null
        }

        // 先尝试解析为 JsonElement，支持对象或数组
        try {
            com.google.gson.JsonElement je = gson.fromJson(s, com.google.gson.JsonElement.class);
            if (je == null) return null;
            Map<String, List<String>> tmp = new LinkedHashMap<>();
            if (je.isJsonObject()) {
                com.google.gson.JsonObject jo = je.getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : jo.entrySet()) {
                    String day = entry.getKey().trim();
                    com.google.gson.JsonElement val = entry.getValue();
                    if (val == null || day.isEmpty()) continue;
                    if (val.isJsonArray()) {
                        // 值为数组，逐项加入
                        for (com.google.gson.JsonElement item : val.getAsJsonArray()) {
                            String t = item.isJsonNull() ? "" : item.getAsString().trim();
                            if (!t.isEmpty()) {
                                tmp.computeIfAbsent(day, k -> new ArrayList<>());
                                if (!tmp.get(day).contains(t)) tmp.get(day).add(t);
                            }
                        }
                    } else if (val.isJsonPrimitive()) {
                        String t = val.getAsString().trim();
                        if (!t.isEmpty()) {
                            tmp.computeIfAbsent(day, k -> new ArrayList<>());
                            if (!tmp.get(day).contains(t)) tmp.get(day).add(t);
                        }
                    } else if (val.isJsonObject()) {
                        // 如果值是对象，尝试拿 time 字段
                        com.google.gson.JsonObject vobj = val.getAsJsonObject();
                        if (vobj.has("time")) {
                            String t = vobj.get("time").getAsString().trim();
                            if (!t.isEmpty()) {
                                tmp.computeIfAbsent(day, k -> new ArrayList<>());
                                if (!tmp.get(day).contains(t)) tmp.get(day).add(t);
                            }
                        }
                    }
                }
            } else if (je.isJsonArray()) {
                // 支持数组形式: [{"day":"周六","time":"1-2节"}, ...]
                for (com.google.gson.JsonElement el : je.getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    String day = null;
                    String time = null;
                    if (obj.has("day")) day = obj.get("day").getAsString().trim();
                    if (obj.has("time")) time = obj.get("time").getAsString().trim();
                    // 兼容早期可能使用 fields 名称不同
                    if ((day == null || day.isEmpty()) && obj.has("d")) day = obj.get("d").getAsString().trim();
                    if ((time == null || time.isEmpty()) && obj.has("t")) time = obj.get("t").getAsString().trim();
                    if (day == null || day.isEmpty() || time == null || time.isEmpty()) continue;
                    tmp.computeIfAbsent(day, k -> new ArrayList<>());
                    if (!tmp.get(day).contains(time)) tmp.get(day).add(time);
                }
            } else {
                // 既不是对象也不是数组，尝试解析为 Map<String,String>
                try {
                    java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                    Map<String, String> m = gson.fromJson(s, mapType);
                    if (m != null) {
                        for (Map.Entry<String, String> e : m.entrySet()) {
                            if (e.getKey() == null) continue;
                            String day = e.getKey().trim();
                            String t = e.getValue() == null ? "" : e.getValue().trim();
                            if (!day.isEmpty() && !t.isEmpty()) {
                                tmp.computeIfAbsent(day, k -> new ArrayList<>());
                                if (!tmp.get(day).contains(t)) tmp.get(day).add(t);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // 继续到失败处理
                }
            }

            if (tmp.isEmpty()) return null;
            // 把 List<String> 转为逗号连接的单个字符串（后端数据库当前只保存字符串）
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> en : tmp.entrySet()) {
                List<String> vals = en.getValue();
                // 保持原始顺序，且去重
                List<String> clean = new ArrayList<>();
                for (String it : vals) if (it != null && !it.trim().isEmpty() && !clean.contains(it.trim())) clean.add(it.trim());
                if (!clean.isEmpty()) out.put(en.getKey(), String.join(",", clean));
            }
            if (out.isEmpty()) return null;
            return gson.toJson(out);
        } catch (Exception e) {
            System.err.println("normalizeScheduleForStorage 解析失败: " + e.getMessage());
            return null;
        }
    }
}
