package Server.dao.book;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import Server.model.book.Book;

public interface BookMapper {

    @Select("SELECT * FROM book WHERE isbn = #{isbn}")
    Book findByIsbn(@Param("isbn") String isbn);

    @Select("SELECT * FROM book WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Book> findByName(@Param("name") String name);

    @Select("SELECT * FROM book WHERE author LIKE CONCAT('%', #{author}, '%')")
    List<Book> findByAuthor(@Param("author") String author);

    @Select("SELECT * FROM book WHERE description LIKE CONCAT('%', #{description}, '%')")
    List<Book> findByDescription(@Param("description") String description);

    @Select("SELECT * FROM book WHERE inventory BETWEEN #{min} AND #{max}")
    List<Book> findByInventoryRange(@Param("min") int min, @Param("max") int max);

    @Insert("INSERT INTO book(name, isbn, author, publisher, publish_date, description, inventory, category) " +
            "VALUES(#{name}, #{isbn}, #{author}, #{publisher}, #{publishDate}, #{description}, #{inventory}, #{category})")
    int insertBook(Book book);

    @Update("UPDATE book SET name=#{name}, author=#{author}, publisher=#{publisher}, " +
            "publish_date=#{publishDate}, description=#{description}, inventory=#{inventory}, category=#{category} " +
            "WHERE isbn=#{isbn}")
    int updateBook(Book book);

    @Delete("DELETE FROM book WHERE isbn=#{isbn}")
    int deleteBook(@Param("isbn") String isbn);
}
