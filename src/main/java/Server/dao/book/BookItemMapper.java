package Server.dao.book;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import Server.model.book.BookItem;

public interface BookItemMapper {

    @Select("SELECT * FROM book_item WHERE uuid = #{uuid}")
    BookItem findByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM book_item WHERE isbn = #{isbn}")
    List<BookItem> findByIsbn(@Param("isbn") String isbn);

    // 查询可借副本
    @Select("SELECT * FROM book_item WHERE uuid = #{uuid} AND book_status='INLIBRARY' LIMIT 1")
    BookItem findAvailableByUuid(@Param("uuid") String uuid);

    @Insert("INSERT INTO book_item(uuid, isbn, place, book_status) VALUES(#{uuid}, #{isbn}, #{place}, #{bookStatus})")
    int insertBookItem(BookItem bookItem);

    @Update("UPDATE book_item SET place=#{place}, book_status=#{bookStatus} WHERE uuid=#{uuid}")
    int updateBookItem(BookItem bookItem);

    @Delete("DELETE FROM book_item WHERE isbn=#{isbn}")
    int deleteBookItemIsbn(@Param("isbn") String isbn);

    @Delete("DELETE FROM book_item WHERE uuid=#{uuid}")
    int deleteBookItem(@Param("uuid") String uuid);
}
