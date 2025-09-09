package Server.model.student;

import java.util.List;

public class ClassStudent {
    private Integer cardNumber;      // 学生一卡通号，9位数字
    private Integer studentNumber;   // 学号，8位数字
    private String major;            // 专业
    private String school;           // 学院
    private String status;           // 学籍状态
    private List<TeachingClass> selectedClasses; // 已选课程列表
    
    // 构造方法
    public ClassStudent() {}
    
    public ClassStudent(Integer cardNumber, Integer studentNumber, String major, String school, String status) {
        this.cardNumber = cardNumber;
        this.studentNumber = studentNumber;
        this.major = major;
        this.school = school;
        this.status = status;
    }
    
    // Getter和Setter方法
    public Integer getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public Integer getStudentNumber() {
        return studentNumber;
    }
    
    public void setStudentNumber(Integer studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public String getSchool() {
        return school;
    }
    
    public void setSchool(String school) {
        this.school = school;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<TeachingClass> getSelectedClasses() {
        return selectedClasses;
    }
    
    public void setSelectedClasses(List<TeachingClass> selectedClasses) {
        this.selectedClasses = selectedClasses;
    }
    
    @Override
    public String toString() {
        return "ClassStudent{" +
                "cardNumber=" + cardNumber +
                ", studentNumber=" + studentNumber +
                ", major='" + major + '\'' +
                ", school='" + school + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
