package Client.model.course;

import java.util.List;

public class ClassStudent {
    private Integer cardNumber;      // 学生一卡通号，9位数字
    private String studentNumber;   // 学号，保存为字符串以保留前导零
    private String name;            // 学生姓名
    private String major;            // 专业
    private String school;           // 学院
    private String status;           // 学籍状态
    private List<TeachingClass> selectedClasses; // 已选课程列表

    // 构造方法
    public ClassStudent() {}
    
    public ClassStudent(Integer cardNumber, String studentNumber, String name, String major, String school, String status) {
        this.cardNumber = cardNumber;
        this.studentNumber = studentNumber;
        this.name = name;
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
    
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
                ", studentNumber='" + studentNumber + '\'' +
                ", name='" + name + '\'' +
                ", major='" + major + '\'' +
                ", school='" + school + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
