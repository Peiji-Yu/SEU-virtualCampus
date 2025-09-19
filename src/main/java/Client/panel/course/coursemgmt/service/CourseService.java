package Client.panel.course.coursemgmt.service;

import Client.ClientNetworkHelper;
import Client.model.Request;
import Client.model.Response;
import Client.model.course.TeachingClass;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 与后端交互的封装层：把网络请求和后端返回的数据解析集中在此处，UI 层调用更语义化的方法。
 */
public class CourseService {
    private static final Gson G = new Gson();

    public static List<Map<String, Object>> fetchAllCourses() throws Exception {
        Request req = new Request("getAllCourses", new HashMap<>());
        String respStr = ClientNetworkHelper.send(req);
        Response resp = G.fromJson(respStr, Response.class);
        if (resp.getCode() != 200) throw new RuntimeException("getAllCourses failed: " + resp.getMessage());
        List<Map<String, Object>> courseList = new ArrayList<>();
        Object dataObj = resp.getData();
        if (dataObj != null) {
            com.google.gson.reflect.TypeToken<List<Map<String, Object>>> token = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){};
            List<Map<String, Object>> parsed = G.fromJson(G.toJson(dataObj), token.getType());
            if (parsed != null) courseList.addAll(parsed);
        }
        return courseList;
    }

    public static List<TeachingClass> fetchTeachingClassesByCourseId(String courseId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        Request tcReq = new Request("getTeachingClassesByCourseId", data);
        String tcRespStr = ClientNetworkHelper.send(tcReq);
        Response tcResp = G.fromJson(tcRespStr, Response.class);
        List<TeachingClass> list = new ArrayList<>();
        if (tcResp.getCode() == 200 && tcResp.getData() != null) {
            Object tcData = tcResp.getData();
            if (tcData instanceof List) {
                list = G.fromJson(G.toJson(tcData), new com.google.gson.reflect.TypeToken<List<TeachingClass>>(){}.getType());
            } else {
                TeachingClass tc = G.fromJson(G.toJson(tcData), TeachingClass.class);
                list.add(tc);
            }
        }
        return list;
    }

    public static Map<String, Object> getTeachingClassStudentsRaw(String teachingClassUuid) throws Exception {
        // reuse existing helper that returns raw JSON object as map
        String resp = ClientNetworkHelper.getTeachingClassStudents(teachingClassUuid);
        Map<String, Object> result = G.fromJson(resp, Map.class);
        return result;
    }

    public static Response addTeachingClass(Map<String, Object> data) throws Exception {
        Request r = new Request("addTeachingClass", data);
        String resp = ClientNetworkHelper.send(r);
        return G.fromJson(resp, Response.class);
    }

    public static Response updateTeachingClass(Map<String, Object> data) throws Exception {
        Request r = new Request("updateTeachingClass", data);
        String resp = ClientNetworkHelper.send(r);
        return G.fromJson(resp, Response.class);
    }

    public static Response deleteTeachingClass(String uuid) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("uuid", uuid);
        Request r = new Request("deleteTeachingClass", data);
        String resp = ClientNetworkHelper.send(r);
        return G.fromJson(resp, Response.class);
    }

    public static Response addCourse(Map<String, Object> data) throws Exception {
        Request r = new Request("addCourse", data);
        String resp = ClientNetworkHelper.send(r);
        return G.fromJson(resp, Response.class);
    }

    public static Response updateCourse(Map<String, Object> data) throws Exception {
        Request r = new Request("updateCourse", data);
        String resp = ClientNetworkHelper.send(r);
        return G.fromJson(resp, Response.class);
    }

    public static Response deleteCourse(String courseId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        Request r = new Request("deleteCourse", data);
        String resp = ClientNetworkHelper.send(r);
        return G.fromJson(resp, Response.class);
    }

    public static Response sendSelectCourse(long cardNumber, String teachingClassUuid) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("teachingClassUuid", teachingClassUuid);
        Request req = new Request("selectCourse", data);
        String resp = ClientNetworkHelper.send(req);
        return G.fromJson(resp, Response.class);
    }

    public static Response sendDropCourse(Double cardNumber, String teachingClassUuid) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("teachingClassUuid", teachingClassUuid);
        Request req = new Request("dropCourse", data);
        String resp = ClientNetworkHelper.send(req);
        return G.fromJson(resp, Response.class);
    }

    // 获取所有教学班（供前端冲突检测使用）
    public static List<TeachingClass> fetchAllTeachingClasses() throws Exception {
        String respStr = ClientNetworkHelper.getAllTeachingClasses();
        Response resp = G.fromJson(respStr, Response.class);
        List<TeachingClass> list = new ArrayList<>();
        if (resp.getCode() == 200 && resp.getData() != null) {
            Object tcData = resp.getData();
            list = G.fromJson(G.toJson(tcData), new com.google.gson.reflect.TypeToken<List<TeachingClass>>(){}.getType());
        }
        return list;
    }
}
