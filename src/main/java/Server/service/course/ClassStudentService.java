package Server.service.course;

import Server.dao.course.ClassStudentMapper;
import Server.dao.course.CourseMapper;
import Server.model.course.ClassStudent;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ClassStudentService {

    private ClassStudentMapper classStudentMapper;

    public ClassStudentService() {
        SqlSession sqlSession = DatabaseUtil.getSqlSession();
        this.classStudentMapper = sqlSession.getMapper(ClassStudentMapper.class);
    }
    public ClassStudent findByCardNumber(Integer cardNumber) {
        try {
            return classStudentMapper.findByCardNumber(cardNumber);
        } catch (Exception e) {
            System.err.println("查询学生信息失败: " + e.getMessage());
            return null;
        }
    }

    public ClassStudent findByStudentNumber(String studentNumber) {
        try {
            return classStudentMapper.findByStudentNumber(studentNumber);
        } catch (Exception e) {
            System.err.println("查询学生信息失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addStudent(ClassStudent student) {
        try {
            // 检查一卡通号是否已存在
            ClassStudent existingStudent = classStudentMapper.findByCardNumber(student.getCardNumber());
            if (existingStudent != null) {
                System.err.println("一卡通号已存在: " + student.getCardNumber());
                return false;
            }
            
            // 检查学号是否已存在
            existingStudent = classStudentMapper.findByStudentNumber(student.getStudentNumber());
            if (existingStudent != null) {
                System.err.println("学号已存在: " + student.getStudentNumber());
                return false;
            }
            
            int result = classStudentMapper.insertStudent(student);
            return result > 0;
        } catch (Exception e) {
            System.err.println("添加学生失败: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudent(ClassStudent student) {
        try {
            // 检查学生是否存在
            ClassStudent existingStudent = classStudentMapper.findByCardNumber(student.getCardNumber());
            if (existingStudent == null) {
                System.err.println("学生不存在: " + student.getCardNumber());
                return false;
            }
            
            int result = classStudentMapper.updateStudent(student);
            return result > 0;
        } catch (Exception e) {
            System.err.println("更新学生失败: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudent(Integer cardNumber) {
        try {
            // 检查学生是否存在
            ClassStudent existingStudent = classStudentMapper.findByCardNumber(cardNumber);
            if (existingStudent == null) {
                System.err.println("学生不存在: " + cardNumber);
                return false;
            }
            
            int result = classStudentMapper.deleteStudent(cardNumber);
            return result > 0;
        } catch (Exception e) {
            System.err.println("删除学生失败: " + e.getMessage());
            return false;
        }
    }

    public List<ClassStudent> getAllStudents() {
        try {
            return classStudentMapper.findAllStudents();
        } catch (Exception e) {
            System.err.println("获取所有学生失败: " + e.getMessage());
            return null;
        }
    }

    public List<ClassStudent> getStudentsByTeachingClassUuid(String teachingClassUuid) {
        try {
            return classStudentMapper.findByTeachingClassUuid(teachingClassUuid);
        } catch (Exception e) {
            System.err.println("根据教学班查询学生失败: " + e.getMessage());
            return null;
        }
    }
}
