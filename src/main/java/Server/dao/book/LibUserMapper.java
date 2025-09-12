package Server.dao.book;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import Server.model.book.LibUser;

public interface LibUserMapper {

    @Select("SELECT * FROM lib_user WHERE user_id = #{userId}")
    LibUser findById(@Param("userId") int userId);

    @Select("SELECT * FROM lib_user")
    List<LibUser> findAll();

    @Insert("INSERT INTO lib_user(user_id, borrowed, max_borrowed, user_status) " +
            "VALUES(#{userId}, #{borrowed}, #{maxBorrowed}, #{userStatus})")
    int insert(LibUser libUser);

    @Update("UPDATE lib_user SET borrowed=#{borrowed}, max_borrowed=#{maxBorrowed}, user_status=#{userStatus} " +
            "WHERE user_id=#{userId}")
    int update(LibUser libUser);

    @Delete("DELETE FROM lib_user WHERE user_id = #{userId}")
    int delete(@Param("userId") int userId);
}
