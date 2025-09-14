package Server.dao.course;

import Server.model.course.Course;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CourseMapper {
    // 根据课程编号查询课程信息
    @Select("SELECT * FROM courses WHERE course_id = #{courseId}")
    Course findByCourseId(@Param("courseId") String courseId);

    // 根据课程名称查询课程信息
    @Select("SELECT * FROM courses WHERE course_name = #{courseName}")
    Course findByCourseName(@Param("courseName") String courseName);

    // 根据学院查询课程
    @Select("SELECT * FROM courses WHERE school = #{school}")
    List<Course> findBySchool(@Param("school") String school);

    // 插入新课程
    @Insert("INSERT INTO courses (course_id, course_name, school, credit) " +
            "VALUES (#{courseId}, #{courseName}, #{school}, #{credit})")
    int insertCourse(Course course);

    // 更新课程信息
    @Update("UPDATE courses SET " +
            "course_name = #{courseName}, school = #{school}, credit = #{credit} " +
            "WHERE course_id = #{courseId}")
    int updateCourse(Course course);

    // 删除课程
    @Delete("DELETE FROM courses WHERE course_id = #{courseId}")
    int deleteCourse(@Param("courseId") String courseId);

    // 获取所有课程
    @Select("SELECT * FROM courses")
    List<Course> findAllCourses();
    
}
