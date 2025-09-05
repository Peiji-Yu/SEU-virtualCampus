package Server.dao;

import Server.model.book.Book;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface BookMapper {

    @Select("SELECT * FROM book WHERE isbn = #{isbn}")
    Book findByIsbn(@Param("isbn") String isbn);

    @Select("SELECT * FROM book WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Book> findByName(@Param("name") String name);

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

