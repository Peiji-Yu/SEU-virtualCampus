package Server.dao.book;

import Server.model.book.BookItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface BookItemMapper {

    @Select("SELECT * FROM book_item WHERE uuid = #{uuid}")
    BookItem findByUuid(@Param("uuid") String uuid);

    @Select("SELECT * FROM book_item WHERE isbn = #{isbn}")
    List<BookItem> findByIsbn(@Param("isbn") String isbn);

    @Insert("INSERT INTO book_item(uuid, isbn, place, book_status) VALUES(#{uuid}, #{isbn}, #{place}, #{bookStatus})")
    int insertBookItem(BookItem bookItem);

    @Update("UPDATE book_item SET place=#{place}, book_status=#{bookStatus} WHERE uuid=#{uuid}")
    int updateBookItem(BookItem bookItem);

    @Delete("DELETE FROM book_item WHERE uuid=#{uuid}")
    int deleteBookItem(@Param("uuid") String uuid);
}
