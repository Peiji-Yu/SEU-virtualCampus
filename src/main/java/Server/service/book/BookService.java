package Server.service.book;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;

import Server.dao.book.BookItemMapper;
import Server.dao.book.BookMapper;
import Server.dao.book.BookRecordMapper;
import Server.model.book.Book;
import Server.model.book.BookItem;
import Server.model.book.BookRecord;
import Server.model.book.BookStatus;
import Server.util.DatabaseUtil;
public class BookService {

    // 根据ISBN检索书籍信息
    public Book retrieveBook(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            return bookMapper.findByIsbn(isbn);
        }
    }

    // 根据书籍名称检索书籍信息
    public List<Book> searchBooks(String name) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            return bookMapper.findByName("%" + name + "%");
        }
    }


    // 根据ISBN检索实体书籍
    public List<BookItem> retrieveBookItems(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            return bookItemMapper.findByIsbn(isbn);
        }
    }

    // 根据UUID检索实体书籍
    public BookItem getBookItemByUuid(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            return bookItemMapper.findByUuid(uuid);
        }
    }

    // 管理员更新书籍实体
    public boolean updateBookItem(BookItem bookItem) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            int res = bookItemMapper.updateBookItem(bookItem);
            sqlSession.commit();
            return res > 0;
        }
    }


    // 管理员更新书籍
    public boolean updateBook(Book book) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            int res = bookMapper.updateBook(book);
            sqlSession.commit();
            return res > 0;
        }
    }

    // 添加书籍实体
    public boolean addBookItem(BookItem bookItem) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);

            // 生成 UUID（如果没有）
            if (bookItem.getUuid() == null || bookItem.getUuid().isEmpty()) {
                bookItem.setUuid(UUID.randomUUID().toString());
            }

            // 插入实体记录
            int res = bookItemMapper.insertBookItem(bookItem);

            sqlSession.commit();
            return res > 0;
        }
    }

    // 管理员添加书籍
    public boolean addBook(Book book) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            int resBook = bookMapper.insertBook(book);
            sqlSession.commit();
            return resBook > 0;
        }
    }

    // 删除书籍实体
    public boolean deleteBookItem(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);

            // 删除实体
            int res = bookItemMapper.deleteBookItem(uuid);

            sqlSession.commit();
            return res > 0;
        }
    }

    // 管理员删除书籍
    public boolean deleteBook(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);

            // 先删除所有副本
            bookItemMapper.deleteBookItemIsbn(isbn);

            // 再删除书籍
            int resbook = bookMapper.deleteBook(isbn);

            sqlSession.commit();
            return resbook > 0;
        }
    }

    // 根据一卡通号查询个人所有借阅记录
    public List<BookRecord> userRecords(int userId) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            return bookRecordMapper.findByUserId(userId);
        }
    }


    // 借书
    public boolean borrowBook(int userId, String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);

            // 3. 查询在馆副本
            BookItem bookItem = bookItemMapper.findAvailableByUuid(uuid);
            if (bookItem == null) {
                return false; // 没有可借副本
            }

            // 4. 更新副本状态为借出
            bookItem.setBookStatus(BookStatus.LEND);
            bookItemMapper.updateBookItem(bookItem);
            Book book = bookMapper.findByIsbn(bookItem.getIsbn());

            // 7. 插入借阅记录
            BookRecord record = new BookRecord();
            record.setName(book.getName());
            record.setUuid(uuid);
            record.setUserId(userId);
            record.setBorrowTime(LocalDate.now());
            record.setDueTime(LocalDate.now().plusDays(30)); // 默认30天
            bookRecordMapper.insertBookRecord(record);
            sqlSession.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 还书
    public boolean returnBook(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);

            // 1. 查询借阅记录
            BookRecord record = bookRecordMapper.findByUuid(uuid);
            if (record == null ) {
                return false; // 记录不存在或已归还
            }

            // 3. 更新副本状态为在馆
            BookItem item = bookItemMapper.findByUuid(record.getUuid());
            if (item != null) {
                item.setBookStatus(BookStatus.INLIBRARY);
                bookItemMapper.updateBookItem(item);
            }

            bookRecordMapper.deleteBookRecord(record.getUuid());
            sqlSession.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 续借
    public boolean renewBook(String recordUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);

            // 1. 查询借阅记录
            BookRecord record = bookRecordMapper.findByUuid(recordUuid);
            if (record == null) {
                return false; // 记录不存在或已归还，不能续借
            }

            // 2. 延长到期时间 30 天
            record.setDueTime(record.getDueTime().plusDays(30));
            bookRecordMapper.updateBookRecord(record);

            sqlSession.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
