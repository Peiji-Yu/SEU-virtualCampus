package Server.dao.book;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import Server.model.book.BookRecord;

public interface BookRecordMapper {

    @Select("SELECT * FROM book_record WHERE uuid = #{uuid}")
    BookRecord findByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM book_record WHERE user_id = #{userId}")
    List<BookRecord> findByUserId(@Param("userId") int userId);

    @Select("SELECT * FROM book_record WHERE isbn = #{isbn}")
    List<BookRecord> findByIsbn(@Param("isbn") String isbn);

    @Insert("INSERT INTO book_record(uuid, user_id, borrow_time, due_time) " +
            "VALUES(#{uuid}, #{userId}, #{borrowTime}, #{dueTime})")
    int insertBookRecord(BookRecord bookRecord);

    @Update("UPDATE book_record SET borrow_time=#{borrowTime}, due_time=#{dueTime} WHERE uuid=#{uuid}")
    int updateBookRecord(BookRecord bookRecord);

    @Delete("DELETE FROM book_record WHERE uuid=#{uuid}")
    int deleteBookRecord(@Param("uuid") String uuid);
}
