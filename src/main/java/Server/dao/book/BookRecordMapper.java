package Server.dao.book;

import Server.model.book.BookRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface BookRecordMapper {
    // 根据 uuid 查询单条借阅记录
    @Select("SELECT * FROM book_record WHERE uuid = #{uuid}")
    BookRecord findByUuid(@Param("uuid") String uuid);

    // 根据 userId 查询该用户的所有借阅记录
    @Select("SELECT * FROM book_record WHERE user_id = #{userId}")
    List<BookRecord> findByUserId(@Param("userId") int userId);

    // 插入借阅记录
    @Insert("INSERT INTO book_record(uuid, user_id, borrow_time, due_time) " +
            "VALUES(#{uuid}, #{userId}, #{borrowTime}, #{dueTime})")
    int insertBookRecord(BookRecord bookRecord);

    // 更新借阅记录（例如修改还书时间）
    @Update("UPDATE book_record SET borrow_time=#{borrowTime}, due_time=#{dueTime} WHERE uuid=#{uuid}")
    int updateBookRecord(BookRecord bookRecord);

    // 删除借阅记录
    @Delete("DELETE FROM book_record WHERE uuid=#{uuid}")
    int deleteBookRecord(@Param("uuid") String uuid);
}

