package Server.dao;

import Server.model.teachingclass.TeachingClass;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TeachingClassMapper {
    // 根据UUID查询教学班信息
    @Select("SELECT * FROM teaching_class WHERE uuid = #{uuid}")
    TeachingClass findByUuid(@Param("uuid") String uuid);
    
    // 根据课程编号查询教学班
    @Select("SELECT * FROM teaching_class WHERE course_id = #{courseId}")
    List<TeachingClass> findByCourseId(@Param("courseId") String courseId);
  
   // 根据教师ID查询教学班
    @Select("SELECT * FROM teaching_class WHERE teacher_id = #{teacherId}")
    List<TeachingClass> findByTeacherId(@Param("teacherId") Integer teacherId);
    
    // 插入新教学班
    @Insert("INSERT INTO teaching_class (uuid, course_id, teacher_id, schedule, place, capacity, selected_count) " +
            "VALUES (#{uuid}, #{courseId}, #{teacherId}, #{schedule}, #{place}, #{capacity}, #{selectedCount})")
    int insertTeachingClass(TeachingClass teachingClass);
    
    // 更新教学班信息
    @Update("UPDATE teaching_class SET " +
            "course_id = #{courseId}, teacher_id = #{teacherId}, schedule = #{schedule}, " +
            "place = #{place}, capacity = #{capacity}, selected_count = #{selectedCount} " +
            "WHERE uuid = #{uuid}")
    int updateTeachingClass(TeachingClass teachingClass);
    
    // 删除教学班
    @Delete("DELETE FROM teaching_class WHERE uuid = #{uuid}")
    int deleteTeachingClass(@Param("uuid") String uuid);
    
    // 获取所有教学班
    @Select("SELECT * FROM teaching_class")
    List<TeachingClass> findAllTeachingClasses();
    
    
    // 增加教学班选课人数
    @Update("UPDATE teaching_class SET selected_count = selected_count + 1 WHERE uuid = #{uuid} AND selected_count < capacity")
    int incrementSelectedCount(@Param("uuid") String uuid);
    
    // 减少教学班选课人数
    @Update("UPDATE teaching_class SET selected_count = selected_count - 1 WHERE uuid = #{uuid} AND selected_count > 0")
    int decrementSelectedCount(@Param("uuid") String uuid);
}
