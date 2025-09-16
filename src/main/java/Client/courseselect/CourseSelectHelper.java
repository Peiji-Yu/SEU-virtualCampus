package Client.courseselect;

import Server.model.course.TeachingClass;
import com.google.gson.Gson;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 选课辅助工具类，处理选课相关的业务逻辑
 */
public class CourseSelectHelper {
    
    private static final Gson gson = new Gson();
    
    /**
     * 检查课程是否已选
     */
    public static boolean isCourseSelected(String teachingClassUuid, Set<String> selectedUuids) {
        if (teachingClassUuid == null || selectedUuids == null) {
            return false;
        }
        return selectedUuids.contains(teachingClassUuid.trim().toLowerCase());
    }
    
    /**
     * 检查课程是否冲突
     */
    public static boolean isCourseConflict(TeachingClass newTc, Map<String, TeachingClass> teachingClassMap, Set<String> selectedUuids) {
        if (newTc == null || newTc.getSchedule() == null || selectedUuids.isEmpty()) {
            return false;
        }
        
        for (String selectedUuid : selectedUuids) {
            if (selectedUuid == null) continue;
            TeachingClass existingTc = teachingClassMap.get(selectedUuid.trim().toLowerCase());
            if (existingTc != null && existingTc.getSchedule() != null) {
                if (schedulesConflict(existingTc.getSchedule(), newTc.getSchedule())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查两个课程时间表是否冲突
     */
    public static boolean schedulesConflict(String schedule1, String schedule2) {
        if (schedule1 == null || schedule2 == null || schedule1.trim().isEmpty() || schedule2.trim().isEmpty()) {
            return false;
        }
        
        try {
            Map<String, String> map1 = parseSchedule(schedule1);
            Map<String, String> map2 = parseSchedule(schedule2);
            
            for (String day : map1.keySet()) {
                if (map2.containsKey(day)) {
                    String time1 = map1.get(day);
                    String time2 = map2.get(day);
                    if (timePeriodsOverlap(time1, time2)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 如果解析失败，比较原始字符串
            return schedule1.equals(schedule2);
        }
        
        return false;
    }
    
    /**
     * 解析时间表字符串为Map
     */
    private static Map<String, String> parseSchedule(String schedule) {
        Map<String, String> result = new HashMap<>();
        try {
            // 尝试解析为JSON
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
            Map<String, String> jsonMap = gson.fromJson(schedule, mapType);
            if (jsonMap != null && !jsonMap.isEmpty()) {
                return jsonMap;
            }
        } catch (Exception e) {
            // 如果不是JSON格式，按行解析
            String[] lines = schedule.split("\n");
            for (String line : lines) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    result.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return result;
    }
    
    /**
     * 检查两个时间段是否重叠
     */
    private static boolean timePeriodsOverlap(String time1, String time2) {
        if (time1 == null || time2 == null) return false;
        
        // 提取时间段中的节数信息
        Set<Integer> periods1 = extractPeriods(time1);
        Set<Integer> periods2 = extractPeriods(time2);
        
        // 检查是否有重叠的节数
        for (Integer period : periods1) {
            if (periods2.contains(period)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 从时间字符串中提取节数
     */
    private static Set<Integer> extractPeriods(String time) {
        Set<Integer> periods = new HashSet<>();
        
        // 匹配数字模式
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(time);
        
        while (matcher.find()) {
            try {
                int period = Integer.parseInt(matcher.group());
                periods.add(period);
            } catch (NumberFormatException e) {
                // 忽略非数字
            }
        }
        
        return periods;
    }
    
    /**
     * 格式化时间表值，添加具体时间
     */
    public static String formatScheduleValue(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        if (s.contains(":")) return s;

        String[] periodStart = new String[]{"", "08:00", "08:50", "10:00", "10:50", "14:00", "14:50", "15:50", "16:40", "19:00", "19:50", "20:10", "20:55"};
        String[] periodEnd = new String[]{"", "08:45", "09:35", "10:45", "11:30", "14:45", "15:35", "16:35", "17:25", "19:45", "20:35", "20:50", "21:40"};

        Pattern rangePat = Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*节?$");
        Matcher m = rangePat.matcher(s);
        if (m.find()) {
            try {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                if (a < 1) a = 1;
                if (b < 1) b = 1;
                if (a >= periodStart.length) a = periodStart.length - 1;
                if (b >= periodEnd.length) b = periodEnd.length - 1;
                if (a > b) { int tmp=a; a=b; b=tmp; }
                String start = periodStart[a];
                String end = periodEnd[b];
                if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                    return s + " (" + start + "-" + end + ")";
                }
            } catch (Exception ignored) {}
        }

        Pattern singlePat = Pattern.compile("^(\\d+)\\s*节?$");
        m = singlePat.matcher(s);
        if (m.find()) {
            try {
                int p = Integer.parseInt(m.group(1));
                if (p < 1) p = 1;
                if (p >= periodStart.length) p = periodStart.length - 1;
                String start = periodStart[p];
                String end = periodEnd[p];
                if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                    return s + " (" + start + "-" + end + ")";
                }
            } catch (Exception ignored) {}
        }

        return s;
    }
}