package Server.service.course;

import Server.dao.course.TeacherMapper;
import Server.model.course.Teacher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class TeacherService {

    private TeacherMapper teacherMapper;

    public Teacher findByTeacherId(Integer teacherId) {
        try {
            return teacherMapper.findByTeacherId(teacherId);
        } catch (Exception e) {
            System.err.println("查询教师信息失败: " + e.getMessage());
            return null;
        }
    }

    public List<Teacher> findByName(String name) {
        try {
            return teacherMapper.findByName(name);
        } catch (Exception e) {
            System.err.println("查询教师信息失败: " + e.getMessage());
            return null;
        }
    }

    public List<Teacher> findBySchool(String school) {
        try {
            return teacherMapper.findBySchool(school);
        } catch (Exception e) {
            System.err.println("根据学院查询教师失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addTeacher(Teacher teacher) {
        try {
            // 检查教师ID是否已存在
            Teacher existingTeacher = teacherMapper.findByTeacherId(teacher.getTeacherId());
            if (existingTeacher != null) {
                System.err.println("教师ID已存在: " + teacher.getTeacherId());
                return false;
            }

            int result = teacherMapper.insertTeacher(teacher);
            return result > 0;
        } catch (Exception e) {
            System.err.println("添加教师失败: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTeacher(Teacher teacher) {
        try {
            // 检查教师是否存在
            Teacher existingTeacher = teacherMapper.findByTeacherId(teacher.getTeacherId());
            if (existingTeacher == null) {
                System.err.println("教师不存在: " + teacher.getTeacherId());
                return false;
            }

            int result = teacherMapper.updateTeacher(teacher);
            return result > 0;
        } catch (Exception e) {
            System.err.println("更新教师失败: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTeacher(Integer teacherId) {
        try {
            // 检查教师是否存在
            Teacher existingTeacher = teacherMapper.findByTeacherId(teacherId);
            if (existingTeacher == null) {
                System.err.println("教师不存在: " + teacherId);
                return false;
            }

            int result = teacherMapper.deleteTeacher(teacherId);
            return result > 0;
        } catch (Exception e) {
            System.err.println("删除教师失败: " + e.getMessage());
            return false;
        }
    }

    public List<Teacher> getAllTeachers() {
        try {
            return teacherMapper.findAllTeachers();
        } catch (Exception e) {
            System.err.println("获取所有教师失败: " + e.getMessage());
            return null;
        }
    }
}
