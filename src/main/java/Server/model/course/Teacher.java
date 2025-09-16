package Server.model.course;

import java.util.List;

public class Teacher {
    private Integer teacherId;     // 教师ID
    private String name;           // 教师姓名
    private String school;         // 所属学院
    private String title;          // 职称
    private List<TeachingClass> teachingClasses; // 负责的教学班列表

    // 构造方法
    public Teacher() {}

    public Teacher(Integer teacherId, String name, String school, String title) {
        this.teacherId = teacherId;
        this.name = name;
        this.school = school;
        this.title = title;
    }

    // Getter和Setter方法
    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<TeachingClass> getTeachingClasses() {
        return teachingClasses;
    }

    public void setTeachingClasses(List<TeachingClass> teachingClasses) {
        this.teachingClasses = teachingClasses;
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "teacherId=" + teacherId +
                ", name='" + name + '\'' +
                ", school='" + school + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
