package Server.service.course;

import Server.dao.course.CourseMapper;
import Server.model.course.Course;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;
import java.util.List;

public class CourseService {

    private CourseMapper courseMapper;

    public CourseService() {
        SqlSession sqlSession = DatabaseUtil.getSqlSession();
        this.courseMapper = sqlSession.getMapper(CourseMapper.class);
    }

    public Course findByCourseId(String courseId) {
        try {
            return courseMapper.findByCourseId(courseId);
        } catch (Exception e) {
            System.err.println("查询课程信息失败: " + e.getMessage());
            return null;
        }
    }

    public Course findByCourseName(String courseName) {
        try {
            return courseMapper.findByCourseName(courseName);
        } catch (Exception e) {
            System.err.println("查询课程信息失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addCourse(Course course) {
        try {
            // 检查课程ID是否已存在
            Course existingCourse = courseMapper.findByCourseId(course.getCourseId());
            if (existingCourse != null) {
                System.err.println("课程ID已存在: " + course.getCourseId());
                return false;
            }
            
            int result = courseMapper.insertCourse(course);
            return result > 0;
        } catch (Exception e) {
            System.err.println("添加课程失败: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCourse(Course course) {
        try {
            // 检查课程是否存在
            Course existingCourse = courseMapper.findByCourseId(course.getCourseId());
            if (existingCourse == null) {
                System.err.println("课程不存在: " + course.getCourseId());
                return false;
            }
            
            int result = courseMapper.updateCourse(course);
            return result > 0;
        } catch (Exception e) {
            System.err.println("更新课程失败: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCourse(String courseId) {
        try {
            // 检查课程是否存在
            Course existingCourse = courseMapper.findByCourseId(courseId);
            if (existingCourse == null) {
                System.err.println("课程不存在: " + courseId);
                return false;
            }
            
            int result = courseMapper.deleteCourse(courseId);
            return result > 0;
        } catch (Exception e) {
            System.err.println("删除课程失败: " + e.getMessage());
            return false;
        }
    }

    public List<Course> getAllCourses() {
        try {
            return courseMapper.findAllCourses();
        } catch (Exception e) {
            System.err.println("获取所有课程失败: " + e.getMessage());
            return null;
        }
    }

    public List<Course> getCoursesBySchool(String school) {
        try {
            return courseMapper.findBySchool(school);
        } catch (Exception e) {
            System.err.println("根据学院查询课程失败: " + e.getMessage());
            return null;
        }
    }
}
