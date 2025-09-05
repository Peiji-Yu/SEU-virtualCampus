package Server.dao;
import Server.model.book.LibUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface LibUserMapper {
    // 根据 userId 查询用户
    @Select("SELECT * FROM lib_user WHERE user_id = #{userId}")
    LibUser findByUserId(@Param("userId") int userId);

    // 查询所有用户
    @Select("SELECT * FROM lib_user")
    List<LibUser> findAllUsers();

    // 插入新用户
    @Insert("INSERT INTO lib_user(user_id, borrowed, max_borrowed, user_status) " +
            "VALUES(#{userId}, #{borrowed}, #{maxBorrowed}, #{userStatus})")
    int insertLibUser(LibUser libUser);

    // 更新用户信息（如借书数量、状态）
    @Update("UPDATE lib_user SET borrowed=#{borrowed}, max_borrowed=#{maxBorrowed}, user_status=#{userStatus} " +
            "WHERE user_id=#{userId}")
    int updateLibUser(LibUser libUser);

    // 删除用户
    @Delete("DELETE FROM lib_user WHERE user_id = #{userId}")
    int deleteLibUser(@Param("userId") int userId);
}


