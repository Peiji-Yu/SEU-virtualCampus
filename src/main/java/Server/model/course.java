package Server.model.course;

import java.util.List;

public class Course {
    private String courseId;     // 课程编号
    private String courseName;   // 课程名
    private String school;       // 开设学院
    private float credit;        // 学分
    private List<TeachingClass> teachingClasses; // 关联的教学班列表
    
    // 构造方法
    public Course() {}
    
    public Course(String courseId, String courseName, String school, float credit) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.school = school;
        this.credit = credit;
    }
    
    // Getter和Setter方法
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getSchool() {
        return school;
    }
    
    public void setSchool(String school) {
        this.school = school;
    }
    
    public float getCredit() {
        return credit;
    }
    
    public void setCredit(float credit) {
        this.credit = credit;
    }
    
    public List<TeachingClass> getTeachingClasses() {
        return teachingClasses;
    }
    
    public void setTeachingClasses(List<TeachingClass> teachingClasses) {
        this.teachingClasses = teachingClasses;
    }
    
    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", school='" + school + '\'' +
                ", credit=" + credit +
                '}';
    }
}
