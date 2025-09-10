package Server.dao;

import Server.model.course.ClassStudent;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ClassStudentMapper {
    // 根据一卡通号查询学生信息
    @Select("SELECT * FROM student WHERE card_number = #{cardNumber}")
    ClassStudent findByCardNumber(@Param("cardNumber") Integer cardNumber);
    
    // 根据学号查询学生信息
    @Select("SELECT * FROM student WHERE student_number = #{studentNumber}")
    ClassStudent findByStudentNumber(@Param("studentNumber") Integer studentNumber);
    
    // 插入新学生
    @Insert("INSERT INTO student (card_number, student_number, major, school, status) " +
            "VALUES (#{cardNumber}, #{studentNumber}, #{major}, #{school}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "cardNumber")
    int insertStudent(ClassStudent student);
    
    // 更新学生信息
    @Update("UPDATE student SET " +
            "student_number = #{studentNumber}, major = #{major}, school = #{school}, status = #{status} " +
            "WHERE card_number = #{cardNumber}")
    int updateStudent(ClassStudent student);
    
    // 删除学生
    @Delete("DELETE FROM student WHERE card_number = #{cardNumber}")
    int deleteStudent(@Param("cardNumber") Integer cardNumber);
    
    // 获取所有学生
    @Select("SELECT * FROM student")
    List<ClassStudent> findAllStudents();
    
    // 获取最大的一卡通号，用于生成新的一卡通号
    @Select("SELECT MAX(card_number) FROM student")
    Integer getMaxCardNumber();
    
    // 获取最大的学号，用于生成新的学号
    @Select("SELECT MAX(student_number) FROM student")
    Integer getMaxStudentNumber();
    
    // 根据教学班UUID查询选课学生
    @Select("SELECT s.* FROM student s " +
            "JOIN student_teaching_class stc ON s.card_number = stc.student_card_number " +
            "WHERE stc.teaching_class_uuid = #{teachingClassUuid}")
    List<ClassStudent> findByTeachingClassUuid(@Param("teachingClassUuid") String teachingClassUuid);
}
