package Server.service;

import Server.dao.BookMapper;
import Server.model.book.Book;
import Server.dao.BookItemMapper;
import Server.model.book.BookItem;
import Server.dao.BookRecordMapper;
import Server.model.book.BookRecord;
import Server.dao.LibUserMapper;
import Server.dao.StudentMapper;
import Server.model.book.LibUser;
import Server.util.DatabaseUtil;
import java.util.List;
import org.apache.ibatis.session.SqlSession;

public class BookService {
    public Book retrieveBook(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            return bookMapper.findByIsbn(isbn);
        }
    }

    public boolean updateBook(Book book) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            int res = bookMapper.updateBook(book);
            sqlSession.commit();
            return res > 0;
        }
    }

    public boolean addBook(Book book) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            int res = bookMapper.insertBook(book);
            sqlSession.commit();
            return res > 0;
        }
    }

    public boolean deleteBook(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            int res = bookMapper.deleteBook(isbn);
            sqlSession.commit();
            return res > 0;
        }
    }
    public List<BookRecord> userRecords(int userId) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            return bookRecordMapper.findByUserId(userId);
        }
    }


}