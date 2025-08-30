package Server.service;

import Server.dao.UserMapper;
import Server.model.Response;
import Server.model.User;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.Map;

/**
 * 用户服务类
 * 处理用户相关的业务逻辑
 */
public class UserService {

    /**
     * 处理用户登录
     * @param data 包含id和password的Map对象
     * @return 登录响应结果
     */
    public Response login(Map<String, Object> data) {
        // 获取用户名和密码
        int id = ((Double) data.get("cardNumber")).intValue();
        String password = (String) data.get("password"); // 假设密码已经加密

        // 获取SqlSession
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            // 获取Mapper
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

            // 查询用户
            User user = userMapper.findByUsernameAndPassword(id, password);

            if (user != null) {
                // 登录成功，返回用户信息（不包含密码）
                user.setPassword(null); // 清除密码
                return Response.success("登录成功", user);
            } else {
                // 登录失败
                return Response.error("用户名或密码错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "服务器内部错误: " + e.getMessage());
        }
    }
}
