package Server.model.teachingclass;

import Server.model.student.ClassStudent;
import Server.model.teachingclass.TeachingClass;

public class StudentTeachingClass {
    private Integer studentCardNumber;   // 学生一卡通号
    private ClassStudent student;        // 关联的学生对象
    private String teachingClassUuid;    // 教学班UUID
    private TeachingClass teachingClass; // 关联的教学班对象
    
    // 构造方法
    public StudentTeachingClass() {}
    
    public StudentTeachingClass(Integer studentCardNumber, String teachingClassUuid) {
        this.studentCardNumber = studentCardNumber;
        this.teachingClassUuid = teachingClassUuid;
    }
    
    // Getter和Setter方法
    public Integer getStudentCardNumber() {
        return studentCardNumber;
    }
    
    public void setStudentCardNumber(Integer studentCardNumber) {
        this.studentCardNumber = studentCardNumber;
    }
    
    public ClassStudent getStudent() {
        return student;
    }
    
    public void setStudent(ClassStudent student) {
        this.student = student;
    }
    
    public String getTeachingClassUuid() {
        return teachingClassUuid;
    }
    
    public void setTeachingClassUuid(String teachingClassUuid) {
        this.teachingClassUuid = teachingClassUuid;
    }
    
    public TeachingClass getTeachingClass() {
        return teachingClass;
    }
    
    public void setTeachingClass(TeachingClass teachingClass) {
        this.teachingClass = teachingClass;
    }
    
    @Override
    public String toString() {
        return "StudentTeachingClass{" +
                "studentCardNumber=" + studentCardNumber +
                ", teachingClassUuid='" + teachingClassUuid + '\'' +
                '}';
    }
}
