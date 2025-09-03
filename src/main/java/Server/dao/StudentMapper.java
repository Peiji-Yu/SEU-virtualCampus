package Server.dao;

import Server.model.student.SearchType;
import Server.model.student.Student;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface StudentMapper {
    // 根据一卡通号查询学生信息
    @Select("SELECT * FROM student WHERE card_number = #{cardNumber}")
    Student findByCardNumber(@Param("cardNumber") Integer cardNumber);

    // 根据一卡通号/学号/姓名搜索学生信息
    @Select("<script>" +
            "SELECT * FROM student WHERE 1=1" +
            "<if test='searchType == \"byName\"'>" +
            "   <choose>" +
            "       <when test='fuzzy == true'> AND name LIKE CONCAT('%', #{searchValue}, '%')</when>" +
            "       <otherwise> AND name = #{searchValue}</otherwise>" +
            "   </choose>" +
            "</if>" +
            "<if test='searchType == \"byStudentNumber\"'> AND student_number = #{searchValue}</if>" +
            "<if test='searchType == \"byCardNumber\"'> AND card_number = #{searchValue}</if>" +
            "</script>")
    List<Student> searchStudents(@Param("searchType") SearchType searchType,
                                 @Param("searchValue") String searchValue,
                                 @Param("fuzzy") Boolean fuzzy);

    // 插入新学生
    @Insert("INSERT INTO student (identity, card_number, student_number, major, school, " +
            "status, enrollment, birth, birth_place, political_stat, gender, name) " +
            "VALUES (#{identity}, #{cardNumber}, #{studentNumber}, #{major}, #{school}, " +
            "#{status}, #{enrollment}, #{birth}, #{birthPlace}, #{politicalStat}, #{gender}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "cardNumber")
    int insertStudent(Student student);

    // 更新学生信息（排除不可修改字段）
    @Update("UPDATE student SET " +
            "major = #{major}, school = #{school}, status = #{status}, enrollment = #{enrollment}, " +
            "birth_place = #{birthPlace}, political_stat = #{politicalStat} " +
            "WHERE card_number = #{cardNumber}")
    int updateStudent(Student student);

    // 删除学生
    @Delete("DELETE FROM student WHERE card_number = #{cardNumber}")
    int deleteStudent(@Param("cardNumber") Integer cardNumber);

    // 获取最大的一卡通号，用于生成新的一卡通号
    @Select("SELECT MAX(card_number) FROM student")
    Integer getMaxCardNumber();
}
