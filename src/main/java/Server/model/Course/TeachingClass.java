package Server.model.teachingclass;

import Server.model.course.Course;
import Server.model.student.Student;
import java.util.List;

public class TeachingClass {
    private String uuid;         // 唯一标识符，用作主键
    private String courseId;     // 课程编号
    private Course course;       // 关联的课程对象
    private Integer teacherId;   // 教师ID
    private String schedule;     // 上课信息（JSON格式）
    private String place;        // 上课地点
    private Integer capacity;    // 课容量
    private Integer selectedCount; // 选课人数
    private List<Student> students; // 选课学生列表
    
    // 构造方法
    public TeachingClass() {}
    
    public TeachingClass(String uuid, String courseId, Integer teacherId, String schedule, 
                        String place, Integer capacity, Integer selectedCount) {
        this.uuid = uuid;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.schedule = schedule;
        this.place = place;
        this.capacity = capacity;
        this.selectedCount = selectedCount;
    }
    
    // Getter和Setter方法
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public Course getCourse() {
        return course;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }
    
    public Integer getTeacherId() {
        return teacherId;
    }
    
    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }
    
    public String getSchedule() {
        return schedule;
    }
    
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
    
    public String getPlace() {
        return place;
    }
    
    public void setPlace(String place) {
        this.place = place;
    }
    
    public Integer getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    
    public Integer getSelectedCount() {
        return selectedCount;
    }
    
    public void setSelectedCount(Integer selectedCount) {
        this.selectedCount = selectedCount;
    }
    
    public List<Student> getStudents() {
        return students;
    }
    
    public void setStudents(List<Student> students) {
        this.students = students;
    }
    
    @Override
    public String toString() {
        return "TeachingClass{" +
                "uuid='" + uuid + '\'' +
                ", courseId='" + courseId + '\'' +
                ", teacherId=" + teacherId +
                ", schedule='" + schedule + '\'' +
                ", place='" + place + '\'' +
                ", capacity=" + capacity +
                ", selectedCount=" + selectedCount +
                '}';
    }
}
