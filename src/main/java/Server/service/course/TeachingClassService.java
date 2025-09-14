package Server.service.course;

import Server.dao.course.CourseMapper;
import Server.dao.course.TeachingClassMapper;
import Server.model.course.TeachingClass;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class TeachingClassService {

    public TeachingClassService() {
        // 无状态服务，SqlSession 在每个方法中按需获取并关闭，避免连接泄露
    }

    public TeachingClass findByUuid(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper mapper = sqlSession.getMapper(TeachingClassMapper.class);
            return mapper.findByUuid(uuid);
        } catch (Exception e) {
            System.err.println("查询教学班信息失败: " + e.getMessage());
            return null;
        }
    }

    public List<TeachingClass> findByCourseId(String courseId) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            CourseMapper courseMapper = sqlSession.getMapper(CourseMapper.class);
            List<TeachingClass> list = teachingClassMapper.findByCourseId(courseId);
            for (TeachingClass tc : list) {
                // 补充 Course 信息
                tc.setCourse(courseMapper.findByCourseId(tc.getCourseId()));
            }
            return list;
        } catch (Exception e) {
            System.err.println("根据课程ID查询教学班失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addTeachingClass(TeachingClass teachingClass) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            TeachingClass existingTeachingClass = teachingClassMapper.findByUuid(teachingClass.getUuid());
            if (existingTeachingClass != null) {
                System.err.println("教学班UUID已存在: " + teachingClass.getUuid());
                return false;
            }
            int result = teachingClassMapper.insertTeachingClass(teachingClass);
            return result > 0;
        } catch (Exception e) {
            System.err.println("添加教学班失败: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTeachingClass(TeachingClass teachingClass) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            TeachingClass existingTeachingClass = teachingClassMapper.findByUuid(teachingClass.getUuid());
            if (existingTeachingClass == null) {
                System.err.println("教学班不存在: " + teachingClass.getUuid());
                return false;
            }
            int result = teachingClassMapper.updateTeachingClass(teachingClass);
            return result > 0;
        } catch (Exception e) {
            System.err.println("更新教学班失败: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTeachingClass(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            TeachingClass existingTeachingClass = teachingClassMapper.findByUuid(uuid);
            if (existingTeachingClass == null) {
                System.err.println("教学班不存在: " + uuid);
                return false;
            }
            int result = teachingClassMapper.deleteTeachingClass(uuid);
            return result > 0;
        } catch (Exception e) {
            System.err.println("删除教学班失败: " + e.getMessage());
            return false;
        }
    }

    public List<TeachingClass> getAllTeachingClasses() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            return teachingClassMapper.findAllTeachingClasses();
        } catch (Exception e) {
            System.err.println("获取所有教学班失败: " + e.getMessage());
            return null;
        }
    }

    public List<TeachingClass> getTeachingClassesByTeacherName(String teacherName) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            return teachingClassMapper.findByTeacherName(teacherName);
        } catch (Exception e) {
            System.err.println("根据教师姓名查询教学班失败: " + e.getMessage());
            return null;
        }
    }

    public boolean incrementSelectedCount(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            TeachingClass existingTeachingClass = teachingClassMapper.findByUuid(uuid);
            if (existingTeachingClass == null) {
                System.err.println("教学班不存在: " + uuid);
                return false;
            }
            int result = teachingClassMapper.incrementSelectedCount(uuid);
            return result > 0;
        } catch (Exception e) {
            System.err.println("增加选课人数失败: " + e.getMessage());
            return false;
        }
    }

    public boolean decrementSelectedCount(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            TeachingClass existingTeachingClass = teachingClassMapper.findByUuid(uuid);
            if (existingTeachingClass == null) {
                System.err.println("教学班不存在: " + uuid);
                return false;
            }
            int result = teachingClassMapper.decrementSelectedCount(uuid);
            return result > 0;
        } catch (Exception e) {
            System.err.println("减少选课人数失败: " + e.getMessage());
            return false;
        }
    }

    public boolean hasAvailableSeats(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            TeachingClassMapper teachingClassMapper = sqlSession.getMapper(TeachingClassMapper.class);
            TeachingClass teachingClass = teachingClassMapper.findByUuid(uuid);
            if (teachingClass == null) {
                System.err.println("教学班不存在: " + uuid);
                return false;
            }
            return teachingClass.getSelectedCount() < teachingClass.getCapacity();
        } catch (Exception e) {
            System.err.println("检查座位可用性失败: " + e.getMessage());
            return false;
        }
    }
}
