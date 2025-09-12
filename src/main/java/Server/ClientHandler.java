package Server;

import Server.model.*;
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
import Server.service.book.BookService;
import com.google.gson.Gson;

import Server.model.course.Course;
import Server.model.teachingclass.TeachingClass;
import Server.model.student.ClassStudent;
import Server.service.ClassStudentService;
import Server.service.CourseService;
import Server.service.TeachingClassService;
import Server.service.StudentTeachingClassService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 客户端处理器
 * 每个客户端连接都会创建一个此类的实例
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Gson gson = new Gson();
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

                        
                    // 获取所有课程
                    case "getAllCourses":
                        List<Course> courses = courseService.getAllCourses();
                        response = Response.success("获取所有课程成功", courses);
                        break;
                        
                    // 根据学院查询课程
                    case "getCoursesBySchool":
                        String school = (String) request.getData().get("school");
                        List<Course> schoolCourses = courseService.getCoursesBySchool(school);
                        response = Response.success("获取学院课程成功", schoolCourses);
                        break;
                    
                    // 获取所有教学班
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
                    
                    // 根据教师ID获取教学班
                    case "getTeachingClassesByTeacherId":
                        Integer teacherId = ((Double) request.getData().get("teacherId")).intValue();
                        List<TeachingClass> teacherTeachingClasses = teachingClassService.getTeachingClassesByTeacherId(teacherId);
                        response = Response.success("获取教师教学班成功", teacherTeachingClasses);
                        break;
                    
                    // 学生选课
                    case "selectCourse":
                        Integer selectCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        String teachingClassUuid = (String) request.getData().get("teachingClassUuid");
                        
                        try {
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
                            // 获取学生的选课关系
                            List<StudentTeachingClass> studentCourses = studentTeachingClassService.findByStudentCardNumber(studentCardNumber);
                            
                            // 获取教学班详细信息
                            List<TeachingClass> teachingClasses = new ArrayList<>();
                            for (StudentTeachingClass stc : studentCourses) {
                                TeachingClass tc = teachingClassService.findByUuid(stc.getTeachingClassUuid());
                                if (tc != null) {
                                    teachingClasses.add(tc);
                                }
                            }
                            
                            response = Response.success("获取已选课程成功", teachingClasses);
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
                            List<ClassStudent> students = new ArrayList<>();
                            for (StudentTeachingClass stc : classStudents) {
                                ClassStudent student = classStudentService.findByCardNumber(stc.getStudentCardNumber());
                                if (student != null) {
                                    students.add(student);
                                }
                            }
                            
                            response = Response.success("获取教学班学生列表成功", students);
                        } catch (Exception e) {
                            response = Response.error("获取教学班学生列表失败: " + e.getMessage());
                        }
                        break;
                    
                    // 添加课程（管理员功能）
                    case "addCourse":
                        Map<String, Object> courseData = (Map<String, Object>) request.getData().get("course");
                        Course newCourse = createCourseFromMap(courseData);
                        
                        boolean addCourseResult = courseService.addCourse(newCourse);
                        response = addCourseResult ? 
                            Response.success("添加课程成功") : 
                            Response.error("添加课程失败");
                        break;
                    
                    // 更新课程（部分更新）
                    case "updateCourse":
                        String updateCourseId = (String) request.getData().get("courseId");
                        Map<String, Object> courseUpdates = (Map<String, Object>) request.getData().get("updates");
                        
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
                        Map<String, Object> teachingClassData = (Map<String, Object>) request.getData().get("teachingClass");
                        TeachingClass newTeachingClass = createTeachingClassFromMap(teachingClassData);
                        
                        boolean addTeachingClassResult = teachingClassService.addTeachingClass(newTeachingClass);
                        response = addTeachingClassResult ? 
                            Response.success("添加教学班成功") : 
                            Response.error("添加教学班失败");
                        break;
                    
                    // 更新教学班（部分更新）
                    case "updateTeachingClass":
                        String updateUuid = (String) request.getData().get("uuid");
                        Map<String, Object> updates = (Map<String, Object>) request.getData().get("updates");
                        
                        // 获取现有教学班信息
                        TeachingClass existingTeachingClass = teachingClassService.findByUuid(updateUuid);
                        if (existingTeachingClass == null) {
                            response = Response.error("教学班不存在");
                            break;
                        }
                        
                        // 应用更新 - 检查并更新所有可能的字段
                        if (updates.containsKey("courseId")) {
                            existingTeachingClass.setCourseId((String) updates.get("courseId"));
                        }
                        if (updates.containsKey("teacherId")) {
                            existingTeachingClass.setTeacherId(((Double) updates.get("teacherId")).intValue());
                        }
                        if (updates.containsKey("schedule")) {
                            existingTeachingClass.setSchedule((String) updates.get("schedule"));
                        }
                        if (updates.containsKey("place")) {
                            existingTeachingClass.setPlace((String) updates.get("place"));
                        }
                        if (updates.containsKey("capacity")) {
                            existingTeachingClass.setCapacity(((Double) updates.get("capacity")).intValue());
                        }
                        if (updates.containsKey("selectedCount")) {
                            existingTeachingClass.setSelectedCount(((Double) updates.get("selectedCount")).intValue());
                        }
                        
                        // 保存更新
                        boolean updateResult = teachingClassService.updateTeachingClass(existingTeachingClass);
                        response = updateResult ? 
                                Response.success("更新教学班成功") : 
                                Response.error("更新教学班失败");
                        break;
                    
                    // 删除教学班（管理员功能）
                    case "deleteTeachingClass":
                        String deleteTeachingClassUuid = (String) request.getData().get("teachingClassUuid");
                        
                        boolean deleteTeachingClassResult = teachingClassService.deleteTeachingClass(deleteTeachingClassUuid);
                        response = deleteTeachingClassResult ? 
                            Response.success("删除教学班成功") : 
                            Response.error("删除教学班失败");
                        break;    


                    case "getFinanceCard":
                        Integer cardNumber1 = ((Double) request.getData().get("cardNumber")).intValue();
                        FinanceCard financeCard = financeService.getFinanceCard(cardNumber1);
                        response = Response.success("获取一卡通信息成功", financeCard);
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

                    case "getTransactions":
                        Integer transactionCardNumber = ((Double) request.getData().get("cardNumber")).intValue();
                        String transactionType = (String) request.getData().get("type");

                        List<CardTransaction> transactions = financeService.getTransactions(transactionCardNumber, transactionType);
                        response = Response.success("获取交易记录成功", transactions);
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
                            if (item != null) {
                                response = Response.success("获取商品成功", item);
                            } else {
                                response = Response.error("商品不存在");
                            }
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

                    case "createOrder":
                        Map<String, Object> orderData = request.getData();
                        Integer orderCardNumber = ((Double) orderData.get("cardNumber")).intValue();
                        String orderRemark = (String) orderData.get("remark");

                        // 解析订单商品项
                        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) orderData.get("items");
                        List<StoreOrderItem> orderItems = new ArrayList<>();
                        Integer totalAmount = 0;

                        for (Map<String, Object> itemData1 : itemsData) {
                            String itemIdStr1 = (String) itemData1.get("itemId");
                            Integer itemAmount = ((Double) itemData1.get("amount")).intValue();

                            try {
                                UUID itemId = UUID.fromString(itemIdStr1);

                                // 获取商品信息以获取价格
                                StoreItem item = storeService.getItemById(itemId);
                                if (item == null) {
                                    response = Response.error("商品不存在: " + itemIdStr1);
                                    break;
                                }

                                StoreOrderItem orderItem = new StoreOrderItem(null, itemId, item.getPrice(), itemAmount);
                                orderItems.add(orderItem);
                                totalAmount += item.getPrice() * itemAmount;

                            } catch (IllegalArgumentException e) {
                                response = Response.error("商品ID格式不正确: " + itemIdStr1);
                                break;
                            }
                        }

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
                            if (order != null) {
                                response = Response.success("获取订单成功", order);
                            } else {
                                response = Response.error("订单不存在");
                            }
                        } catch (IllegalArgumentException e) {
                            response = Response.error("订单ID格式不正确");
                        }
                        break;

                    case "getSalesStats":
                        // 管理员功能：获取销售统计
                        List<StoreMapper.SalesStats> salesStats = storeService.getSalesStatistics();
                        response = Response.success("获取销售统计成功", salesStats);
                        break;

                    case "getTodaySales":
                        // 管理员功能：获取今日销售总额
                        Integer todaySales = storeService.getTodaySalesRevenue();
                        response = Response.success("获取今日销售总额成功", todaySales);
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
        TeachingClass teachingClass = new TeachingClass();
        
        if (data.containsKey("uuid")) teachingClass.setUuid((String) data.get("uuid"));
        if (data.containsKey("courseId")) teachingClass.setCourseId((String) data.get("courseId"));
        if (data.containsKey("teacherId")) teachingClass.setTeacherId(((Double) data.get("teacherId")).intValue());
        if (data.containsKey("schedule")) teachingClass.setSchedule((String) data.get("schedule"));
        if (data.containsKey("place")) teachingClass.setPlace((String) data.get("place"));
        if (data.containsKey("capacity")) teachingClass.setCapacity(((Double) data.get("capacity")).intValue());
        if (data.containsKey("selectedCount")) teachingClass.setSelectedCount(((Double) data.get("selectedCount")).intValue());
        
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
        if (data.containsKey("barcode")) item.setBarcode((String) data.get("barcode"));

        return item;
    }
}
