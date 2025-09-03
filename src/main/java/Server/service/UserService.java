package Server.service;

import Server.dao.UserMapper;
import Server.model.Response;
import Server.model.login.User;
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
     * @param data 包含cardNumber和password的Map对象
     * @return 登录响应结果
     */
    public Response login(Map<String, Object> data) {
        // 获取用户名和密码
        int cardNumber = ((Double) data.get("cardNumber")).intValue();
        String password = (String) data.get("password"); // 假设密码已经加密

        // 获取SqlSession
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            // 获取Mapper
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

            // 查询用户
            User user = userMapper.findByCardNumberAndPassword(cardNumber, password);

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

    /**
     * 忘记密码验证
     * @param cardNumber 一卡通号
     * @param identity 身份证号
     * @return 验证结果响应
     */
    public Response forgetPassword(Integer cardNumber, String identity) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

            // 查询用户
            User user = userMapper.findByCardNumberAndIdentity(cardNumber, identity);

            if (user != null) {
                // 验证成功，返回成功响应（后续可以添加密码重置逻辑）
                return Response.success("身份验证成功");
            } else {
                // 验证失败
                return Response.error("一卡通号与身份证号不匹配");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 重置用户密码
     * @param cardNumber 一卡通号
     * @param newPassword 新密码
     * @return 重置结果
     */
    public Response resetPassword(Integer cardNumber, String newPassword) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

            // 更新密码
            int result = userMapper.updatePassword(cardNumber, newPassword);
            sqlSession.commit();

            if (result > 0) {
                return Response.success("密码重置成功");
            } else {
                return Response.error("密码重置失败，用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "密码重置过程中发生错误: " + e.getMessage());
        }
    }
}
