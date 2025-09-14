package Server.dao.course;

import Server.model.course.TeachingClass;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TeachingClassMapper {
    // 根据UUID查询教学班信息
    @Select("SELECT * FROM teaching_classes WHERE uuid = #{uuid}")
    @Results({
        @Result(property = "teacherName", column = "teacher_name")
    })
    TeachingClass findByUuid(@Param("uuid") String uuid);
    
    // 根据课程编号查询教学班
    @Select("SELECT * FROM teaching_classes WHERE course_id = #{courseId}")
    @Results({
        @Result(property = "teacherName", column = "teacher_name")
    })
    List<TeachingClass> findByCourseId(@Param("courseId") String courseId);
  
   // 根据教师姓名查询教学班
    @Select("SELECT * FROM teaching_classes WHERE teacher_name = #{teacherName}")
    List<TeachingClass> findByTeacherName(@Param("teacherName") String teacherName);

    // 插入新教学班
    @Insert("INSERT INTO teaching_classes (uuid, course_id, teacher_name, schedule, place, capacity, selected_count) " +
            "VALUES (#{uuid}, #{courseId}, #{teacherName}, #{schedule}, #{place}, #{capacity}, #{selectedCount})")
    int insertTeachingClass(TeachingClass teachingClass);
    
    // 更新教学班信息
    @Update("UPDATE teaching_classes SET " +
            "course_id = #{courseId}, teacher_name = #{teacherName}, schedule = #{schedule}, " +
            "place = #{place}, capacity = #{capacity}, selected_count = #{selectedCount} " +
            "WHERE uuid = #{uuid}")
    int updateTeachingClass(TeachingClass teachingClass);
    
    // 删除教学班
    @Delete("DELETE FROM teaching_classes WHERE uuid = #{uuid}")
    int deleteTeachingClass(@Param("uuid") String uuid);
    
    // 获取所有教学班
    @Select("SELECT * FROM teaching_classes")
    List<TeachingClass> findAllTeachingClasses();
    
    
    // 增加教学班选课人数
    @Update("UPDATE teaching_classes SET selected_count = selected_count + 1 WHERE uuid = #{uuid} AND selected_count < capacity")
    int incrementSelectedCount(@Param("uuid") String uuid);
    
    // 减少教学班选课人数
    @Update("UPDATE teaching_classes SET selected_count = selected_count - 1 WHERE uuid = #{uuid} AND selected_count > 0")
    int decrementSelectedCount(@Param("uuid") String uuid);
}
