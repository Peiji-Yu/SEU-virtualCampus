package Server.dao;

import Server.model.teachingclass.StudentTeachingClass;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface StudentTeachingClassMapper {
    // 查询学生的所有选课
    @Select("SELECT * FROM student_teaching_class WHERE student_card_number = #{studentCardNumber}")
    List<StudentTeachingClass> findByStudentCardNumber(@Param("studentCardNumber") Integer studentCardNumber);
    
    // 查询教学班的所有选课学生
    @Select("SELECT * FROM student_teaching_class WHERE teaching_class_uuid = #{teachingClassUuid}")
    List<StudentTeachingClass> findByTeachingClassUuid(@Param("teachingClassUuid") String teachingClassUuid);
    
    // 查询特定学生和教学班的选课关系
    @Select("SELECT * FROM student_teaching_class WHERE student_card_number = #{studentCardNumber} AND teaching_class_uuid = #{teachingClassUuid}")
    StudentTeachingClass findByStudentAndTeachingClass(@Param("studentCardNumber") Integer studentCardNumber, 
                                                      @Param("teachingClassUuid") String teachingClassUuid);
    
    // 插入选课关系
    @Insert("INSERT INTO student_teaching_class (student_card_number, teaching_class_uuid) " +
            "VALUES (#{studentCardNumber}, #{teachingClassUuid})")
    int insertStudentTeachingClass(StudentTeachingClass studentTeachingClass);
    
    // 删除选课关系
    @Delete("DELETE FROM student_teaching_class WHERE student_card_number = #{studentCardNumber} AND teaching_class_uuid = #{teachingClassUuid}")
    int deleteStudentTeachingClass(@Param("studentCardNumber") Integer studentCardNumber, 
                                  @Param("teachingClassUuid") String teachingClassUuid);
    
    // 统计教学班的选课人数
    @Select("SELECT COUNT(*) FROM student_teaching_class WHERE teaching_class_uuid = #{teachingClassUuid}")
    int countByTeachingClassUuid(@Param("teachingClassUuid") String teachingClassUuid);
    
    // 统计学生的选课数量
    @Select("SELECT COUNT(*) FROM student_teaching_class WHERE student_card_number = #{studentCardNumber}")
    int countByStudentCardNumber(@Param("studentCardNumber") Integer studentCardNumber);
}
