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

    // 根据一卡通号查询教师负责的教学班（通过 user 表的 card_number 与 teaching_classes.teacher_name 关联）
    @Select("SELECT tc.* FROM teaching_classes tc JOIN user u ON tc.teacher_name = u.name WHERE u.card_number = #{cardNumber}")
    @Results({
        @Result(property = "teacherName", column = "teacher_name")
    })
    List<TeachingClass> findByTeacherCardNumber(@Param("cardNumber") Integer cardNumber);

    // 插入新教学班
    @Insert("INSERT INTO teaching_classes (uuid, course_id, teacher_name, schedule, place, capacity, selected_count) " +
            "VALUES (#{uuid}, #{courseId}, #{teacherName}, #{schedule}, #{place}, IFNULL(#{capacity}, 0), IFNULL(#{selectedCount}, 0))")
    int insertTeachingClass(TeachingClass teachingClass);
    
    // 更新教学班信息
    @Update("UPDATE teaching_classes SET " +
            "course_id = #{courseId}, teacher_name = #{teacherName}, schedule = #{schedule}, " +
            "place = #{place}, capacity = IFNULL(#{capacity}, capacity), selected_count = IFNULL(#{selectedCount}, selected_count) " +
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
