package Server.service.course;

import Server.dao.course.TeachingClassMapper;
import Server.model.course.TeachingClass;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class TeachingClassService {

    private TeachingClassMapper teachingClassMapper;

    public TeachingClass findByUuid(String uuid) {
        try {
            return teachingClassMapper.findByUuid(uuid);
        } catch (Exception e) {
            System.err.println("查询教学班信息失败: " + e.getMessage());
            return null;
        }
    }

    public List<TeachingClass> findByCourseId(String courseId) {
        try {
            return teachingClassMapper.findByCourseId(courseId);
        } catch (Exception e) {
            System.err.println("根据课程ID查询教学班失败: " + e.getMessage());
            return null;
        }
    }

    public boolean addTeachingClass(TeachingClass teachingClass) {
        try {
            // 检查教学班UUID是否已存在
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
        try {
            // 检查教学班是否存在
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
        try {
            // 检查教学班是否存在
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
        try {
            return teachingClassMapper.findAllTeachingClasses();
        } catch (Exception e) {
            System.err.println("获取所有教学班失败: " + e.getMessage());
            return null;
        }
    }

    public List<TeachingClass> getTeachingClassesByTeacherId(Integer teacherId) {
        try {
            return teachingClassMapper.findByTeacherId(teacherId);
        } catch (Exception e) {
            System.err.println("根据教师ID查询教学班失败: " + e.getMessage());
            return null;
        }
    }

    public boolean incrementSelectedCount(String uuid) {
        try {
            // 检查教学班是否存在
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
        try {
            // 检查教学班是否存在
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
        try {
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
