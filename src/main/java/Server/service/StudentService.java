package Server.service;

import Server.dao.StudentMapper;
import Server.model.SearchType;
import Server.model.Student;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class StudentService {

    /**
     * 学生查看自己的信息
     */
    public Student getSelf(Integer cardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
            return studentMapper.findByCardNumber(cardNumber);
        }
    }

    /**
     * 搜索学生信息
     */
    public List<Student> search_Students(SearchType searchType, String searchValue, Boolean fuzzy) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);

            // 如果未指定fuzzy参数，使用该搜索类型的默认值
            if (fuzzy == null) {
                fuzzy = searchType.isFuzzyDefault();
            }

            return studentMapper.searchStudents(searchType, searchValue, fuzzy);
        }
    }

    public List<Student> searchStudents(String searchTypeStr, String searchValue, Boolean fuzzy) {
        try {
            SearchType searchType = SearchType.fromValue(searchTypeStr);
            return search_Students(searchType, searchValue, fuzzy);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的搜索类型: " + searchTypeStr);
        }
    }

    /**
     * 更新学生信息
     */
    public boolean updateStudent(Student student) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
            int result = studentMapper.updateStudent(student);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 添加学生
     */
    public boolean addStudent(Student student) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);

            // 生成新的一卡通号
            Integer maxCardNumber = studentMapper.getMaxCardNumber();
            int newCardNumber = (maxCardNumber != null) ? maxCardNumber + 1 : 213230001;
            student.setCardNumber(newCardNumber);

            int result = studentMapper.insertStudent(student);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 删除学生
     */
    public boolean deleteStudent(Integer cardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
            int result = studentMapper.deleteStudent(cardNumber);
            sqlSession.commit();
            return result > 0;
        }
    }

}
