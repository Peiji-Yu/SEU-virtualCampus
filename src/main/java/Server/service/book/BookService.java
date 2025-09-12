package Server.service.book;

import Server.dao.book.BookMapper;
import Server.dao.book.BookItemMapper;
import Server.dao.book.BookRecordMapper;
import Server.dao.book.LibUserMapper;
import Server.model.book.*;
import Server.util.DatabaseUtil;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;

public class BookService {

    // 根据ISBN检索书籍信息
    public Book retrieveBook(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            return bookMapper.findByIsbn(isbn);
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
    public BookItem uuidBookItem(String uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            return bookItemMapper.findByUuid(uuid);
        }
    }

    // 根据书籍名称检索书籍信息
    public List<Book> searchBooks(String name) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            return bookMapper.findByName("%" + name + "%");
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

    // 管理员更新书籍实体
    public boolean updateBookItem(BookItem bookItem) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            int res = bookItemMapper.updateBookItem(bookItem);
            sqlSession.commit();
            return res > 0;
        }
    }

    // 管理员添加书籍
    public boolean addBook(Book book, BookItem bookItem) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);

            // 1. 查询是否已存在该 ISBN
            Book existingBook = bookMapper.findByIsbn(book.getIsbn());

            int resBook = 0;
            if (existingBook == null) {
                // 不存在：插入新书，库存设为1
                book.setInventory(1);
                resBook = bookMapper.insertBook(book);
            } else {
                // 已存在：更新库存量 +1
                existingBook.setInventory(existingBook.getInventory() + 1);
                resBook = bookMapper.updateBook(existingBook);
            }

            // 2. 插入 book_item 记录
            int resItem = bookItemMapper.insertBookItem(bookItem);

            sqlSession.commit();
            return resBook > 0 && resItem > 0;
        }
    }

    // 管理员删除书籍
    public boolean deleteBook(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            int res = bookMapper.deleteBook(isbn);
            sqlSession.commit();
            return res > 0;
        }
    }

    // 根据一卡通号查询个人所有借阅记录
    public List<BookRecord> userRecords(int userId) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            return bookRecordMapper.findByUserId(userId);
        }
    }

    // 根据ISBN查询书籍借阅记录
    public List<BookRecord> isbnRecords(String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            return bookRecordMapper.findByIsbn(isbn);
        }
    }

    // 借书
    public boolean borrowBook(int userId, String isbn) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            LibUserMapper libUserMapper = sqlSession.getMapper(LibUserMapper.class);
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);

            // 1. 查询用户
            LibUser user = libUserMapper.findById(userId);
            if (user == null || user.getBorrowed() >= user.getMaxBorrowed()) {
                return false; // 用户不存在或已达最大借书量
            }

            // 2. 查询书籍
            Book book = bookMapper.findByIsbn(isbn);
            if (book == null || book.getInventory() <= 0) {
                return false; // 书籍不存在或库存不足
            }

            // 3. 查询在馆副本
            BookItem bookItem = bookItemMapper.findAvailableByIsbn(isbn);
            if (bookItem == null) {
                return false; // 没有可借副本
            }

            // 4. 更新副本状态为借出
            bookItem.setBookStatus(BookStatus.LEND);
            bookItemMapper.updateBookItem(bookItem);

            // 5. 更新书籍库存
            book.setInventory(book.getInventory() - 1);
            bookMapper.updateBook(book);

            // 6. 更新用户已借书数量
            user.setBorrowed(user.getBorrowed() + 1);
            libUserMapper.update(user);

            // 7. 插入借阅记录
            BookRecord record = new BookRecord();
            record.setUuid(UUID.randomUUID().toString());
            record.setUserId(userId);
            record.setBookItemUuid(bookItem.getUuid());
            record.setBorrowTime(new Date());
            record.setDueTime(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)); // 默认30天
            bookRecordMapper.insert(record);

            sqlSession.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 还书
    public boolean returnBook(String recordUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            BookRecordMapper bookRecordMapper = sqlSession.getMapper(BookRecordMapper.class);
            BookItemMapper bookItemMapper = sqlSession.getMapper(BookItemMapper.class);
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
            LibUserMapper libUserMapper = sqlSession.getMapper(LibUserMapper.class);

            // 1. 查询借阅记录
            BookRecord record = bookRecordMapper.findByUuid(recordUuid);
            if (record == null || record.getReturnTime() != null) {
                return false; // 记录不存在或已归还
            }

            // 2. 更新归还时间
            record.setReturnTime(new Date());
            bookRecordMapper.update(record);

            // 3. 更新副本状态为在馆
            BookItem item = bookItemMapper.findByUuid(record.getBookItemUuid());
            if (item != null) {
                item.setBookStatus(BookStatus.INLIBRARY);
                bookItemMapper.updateBookItem(item);
            }

            // 4. 更新书籍库存
            Book book = bookMapper.findByIsbn(item.getIsbn());
            if (book != null) {
                book.setInventory(book.getInventory() + 1);
                bookMapper.updateBook(book);
            }

            // 5. 更新用户已借数量
            LibUser user = libUserMapper.findById(record.getUserId());
            if (user != null && user.getBorrowed() > 0) {
                user.setBorrowed(user.getBorrowed() - 1);
                libUserMapper.update(user);
            }

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
            if (record == null || record.getReturnTime() != null) {
                return false; // 记录不存在或已归还，不能续借
            }

            // 2. 延长到期时间 30 天
            record.setDueTime(new Date(record.getDueTime().getTime() + 30L * 24 * 60 * 60 * 1000));
            bookRecordMapper.update(record);

            sqlSession.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
