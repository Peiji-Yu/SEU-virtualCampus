package Server.dao;

import Server.model.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问接口
 * 使用MyBatis注解方式实现SQL映射
 */
public interface UserMapper {
    /**
     * 根据用户名和密码查询用户
     * @param cardNumber 一卡通号
     * @param password 加密后的密码
     * @return 用户对象，如果不存在则返回null
     */
    @Select("SELECT * FROM user WHERE card_number = #{cardNumber} AND password = #{password}")
    User findByCardNumberAndPassword(@Param("cardNumber") int cardNumber, @Param("password") String password);
}

