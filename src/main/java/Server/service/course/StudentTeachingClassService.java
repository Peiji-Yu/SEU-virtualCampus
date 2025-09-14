package Server.service.course;

import Server.dao.course.StudentTeachingClassMapper;
import Server.model.course.StudentTeachingClass;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class StudentTeachingClassService {

    public StudentTeachingClassService() {
        // 无状态服务，SqlSession 在每个方法中按需获取并关闭，避免连接泄露
    }

    public List<StudentTeachingClass> findByStudentCardNumber(Integer studentCardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            return mapper.findByStudentCardNumber(studentCardNumber);
        } catch (Exception e) {
            System.err.println("查询学生选课失败: " + e.getMessage());
            return null;
        }
    }

    public List<StudentTeachingClass> findByTeachingClassUuid(String teachingClassUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            return mapper.findByTeachingClassUuid(teachingClassUuid);
        } catch (Exception e) {
            System.err.println("查询教学班选课学生失败: " + e.getMessage());
            return null;
        }
    }

    public StudentTeachingClass findByStudentAndTeachingClass(Integer studentCardNumber, String teachingClassUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            return mapper.findByStudentAndTeachingClass(studentCardNumber, teachingClassUuid);
        } catch (Exception e) {
            System.err.println("查询选课关系失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addStudentTeachingClass(StudentTeachingClass studentTeachingClass) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            int result = mapper.insertStudentTeachingClass(studentTeachingClass);
            return result > 0;
        } catch (Exception e) {
            System.err.println("添加选课关系失败: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudentTeachingClass(Integer studentCardNumber, String teachingClassUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            int result = mapper.deleteStudentTeachingClass(studentCardNumber, teachingClassUuid);
            return result > 0;
        } catch (Exception e) {
            System.err.println("删除选课关系失败: " + e.getMessage());
            return false;
        }
    }

    public int countByTeachingClassUuid(String teachingClassUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            return mapper.countByTeachingClassUuid(teachingClassUuid);
        } catch (Exception e) {
            System.err.println("统计教学班选课人数失败: " + e.getMessage());
            return -1;
        }
    }

    public int countByStudentCardNumber(Integer studentCardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentTeachingClassMapper mapper = sqlSession.getMapper(StudentTeachingClassMapper.class);
            return mapper.countByStudentCardNumber(studentCardNumber);
        } catch (Exception e) {
            System.err.println("统计学生选课数量失败: " + e.getMessage());
            return -1;
        }
    }
}
