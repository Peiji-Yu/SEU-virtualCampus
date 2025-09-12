package Server.service.student;

import Server.dao.student.StudentMapper;
import Server.model.login.User;
import Server.model.student.SearchType;
import Server.model.student.Student;
import Server.service.login.UserService;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class StudentService {

//    /**
//     * 验证管理员身份
//     */
//    private boolean isAdmin(Integer cardNumber) {
//        // 规定cardNumber小于1000的是管理员
//        return cardNumber != null && cardNumber < 1000;
//    }

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

            // 自动创建用户账号
            if (result > 0) {
                String identity = student.getIdentity();
                String password = String.format("%06d", newCardNumber % 1000000);

                UserService userService = new UserService();
                User user = new User(identity, newCardNumber, password);
                userService.addUser(user);
            }

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
