package Server.dao.course;

import Server.model.course.Teacher;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TeacherMapper {
    // 根据教师ID查询教师信息
    @Select("SELECT * FROM teachers WHERE teacher_id = #{teacherId}")
    Teacher findByTeacherId(@Param("teacherId") Integer teacherId);

    // 根据教师姓名查询教师信息
    @Select("SELECT * FROM teachers WHERE name = #{name}")
    List<Teacher> findByName(@Param("name") String name);

    // 根据学院查询教师
    @Select("SELECT * FROM teachers WHERE school = #{school}")
    List<Teacher> findBySchool(@Param("school") String school);

    // 插入新教师
    @Insert("INSERT INTO teachers (teacher_id, name, school, title) " +
            "VALUES (#{teacherId}, #{name}, #{school}, #{title})")
    int insertTeacher(Teacher teacher);

    // 更新教师信息
    @Update("UPDATE teachers SET " +
            "name = #{name}, school = #{school}, title = #{title} " +
            "WHERE teacher_id = #{teacherId}")
    int updateTeacher(Teacher teacher);

    // 删除教师
    @Delete("DELETE FROM teachers WHERE teacher_id = #{teacherId}")
    int deleteTeacher(@Param("teacherId") Integer teacherId);

    // 获取所有教师
    @Select("SELECT * FROM teachers")
    List<Teacher> findAllTeachers();

    // 获取最大的教师ID，用于生成新的教师ID
    @Select("SELECT MAX(teacher_id) FROM teachers")
    Integer getMaxTeacherId();
}
