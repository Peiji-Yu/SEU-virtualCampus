package Server.dao;

import Server.model.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 根据一卡通号和身份证号查询用户
     * @param cardNumber 一卡通号
     * @param identity 身份证号
     * @return 用户对象，如果不存在则返回null
     */
    @Select("SELECT * FROM user WHERE card_number = #{cardNumber} AND id = #{identity}")
    User findByCardNumberAndIdentity(@Param("cardNumber") Integer cardNumber,
                                     @Param("identity") String identity);

    /**
     * 更新用户密码
     * @param cardNumber 一卡通号
     * @param newPassword 新密码（加密后的）
     * @return 更新影响的行数
     */
    @Update("UPDATE user SET password = #{newPassword} WHERE card_number = #{cardNumber}")
    int updatePassword(@Param("cardNumber") Integer cardNumber,
                       @Param("newPassword") String newPassword);
}

