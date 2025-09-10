package Server.service;

import Server.dao.StudentTeachingClassMapper;
import Server.model.course.StudentTeachingClass;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class StudentTeachingClassService {

    private StudentTeachingClassMapper studentTeachingClassMapper;

    public List<StudentTeachingClass> findByStudentCardNumber(Integer studentCardNumber) {
        try {
            return studentTeachingClassMapper.findByStudentCardNumber(studentCardNumber);
        } catch (Exception e) {
            System.err.println("查询学生选课失败: " + e.getMessage());
            return null;
        }
    }

    public List<StudentTeachingClass> findByTeachingClassUuid(String teachingClassUuid) {
        try {
            return studentTeachingClassMapper.findByTeachingClassUuid(teachingClassUuid);
        } catch (Exception e) {
            System.err.println("查询教学班选课学生失败: " + e.getMessage());
            return null;
        }
    }

    public StudentTeachingClass findByStudentAndTeachingClass(Integer studentCardNumber, String teachingClassUuid) {
        try {
            return studentTeachingClassMapper.findByStudentAndTeachingClass(studentCardNumber, teachingClassUuid);
        } catch (Exception e) {
            System.err.println("查询选课关系失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addStudentTeachingClass(StudentTeachingClass studentTeachingClass) {
        try {
            int result = studentTeachingClassMapper.insertStudentTeachingClass(studentTeachingClass);
            return result > 0;
        } catch (Exception e) {
            System.err.println("添加选课关系失败: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudentTeachingClass(Integer studentCardNumber, String teachingClassUuid) {
        try {
            int result = studentTeachingClassMapper.deleteStudentTeachingClass(studentCardNumber, teachingClassUuid);
            return result > 0;
        } catch (Exception e) {
            System.err.println("删除选课关系失败: " + e.getMessage());
            return false;
        }
    }

    public int countByTeachingClassUuid(String teachingClassUuid) {
        try {
            return studentTeachingClassMapper.countByTeachingClassUuid(teachingClassUuid);
        } catch (Exception e) {
            System.err.println("统计教学班选课人数失败: " + e.getMessage());
            return -1;
        }
    }

    public int countByStudentCardNumber(Integer studentCardNumber) {
        try {
            return studentTeachingClassMapper.countByStudentCardNumber(studentCardNumber);
        } catch (Exception e) {
            System.err.println("统计学生选课数量失败: " + e.getMessage());
            return -1;
        }
    }
}
