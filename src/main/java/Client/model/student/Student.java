package Client.model.student;

import java.util.Date;

public class Student {
    private String identity;         // 身份证号
    private Integer cardNumber;      // 一卡通号
    private String studentNumber;    // 学号
    private String major;            // 专业名称
    private String school;           // 学院名称
    private StudentStatus status;    // 学籍状态
    private Date enrollment;         // 入学日期
    private Date birth;              // 出生日期
    private String birthPlace;       // 籍贯
    private PoliticalStatus politicalStat; // 政治面貌
    private Gender gender;           // 性别
    private String name;             // 姓名

    // 构造方法
    public Student() {}

    // 带参数的构造方法
    public Student(String identity, Integer cardNumber, String studentNumber,
                   String major, String school, StudentStatus status,
                   Date enrollment, Date birth, String birthPlace,
                   PoliticalStatus politicalStat, Gender gender, String name) {
        this.identity = identity;
        this.cardNumber = cardNumber;
        this.studentNumber = studentNumber;
        this.major = major;
        this.school = school;
        this.status = status;
        this.enrollment = enrollment;
        this.birth = birth;
        this.birthPlace = birthPlace;
        this.politicalStat = politicalStat;
        this.gender = gender;
        this.name = name;
    }

    // Getter和Setter方法
    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

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

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public Date getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Date enrollment) {
        this.enrollment = enrollment;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public PoliticalStatus getPoliticalStat() {
        return politicalStat;
    }

    public void setPoliticalStat(PoliticalStatus politicalStat) {
        this.politicalStat = politicalStat;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Student{" +
                "identity='" + identity + '\'' +
                ", cardNumber=" + cardNumber +
                ", studentNumber='" + studentNumber + '\'' +
                ", major='" + major + '\'' +
                ", school='" + school + '\'' +
                ", status=" + status +
                ", enrollment=" + enrollment +
                ", birth=" + birth +
                ", birthPlace='" + birthPlace + '\'' +
                ", politicalStat=" + politicalStat +
                ", gender=" + gender +
                ", name='" + name + '\'' +
                '}';
    }
}
