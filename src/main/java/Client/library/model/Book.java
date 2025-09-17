package Client.library.model;

import java.util.List;

/**
 * 书籍类
 */
public class Book {

    private BookClass book;   // 类别

    private List<BookItem> items; // 该书的所有实体（副本）

    public Book() {
    }

    public Book(BookClass book, List<BookItem> items) {
        this.book = book;
        this.items = items;
    }

    // Getter 和 Setter
    public BookClass getBookClass() {
        return book;
    }
    public void setBookClass(BookClass book) {
        this.book = book;
    }

    public List<BookItem> getItems() {
        return items;
    }
    public void setItems(List<BookItem> items) {
        this.items = items;
    }
}