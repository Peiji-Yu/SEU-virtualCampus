package Server.model.book;

import java.time.LocalDate;
import java.util.List;

/**
 * 书籍类
 */
public class Book {

    private String name;         // 书名
    private String isbn;         // ISBN
    private String author;       // 作者
    private String publisher;    // 出版社
    private LocalDate publishDate; // 出版日期
    private String description;  // 简介
    private int inventory;       // 库存量
    private Category category;   // 类别

    private List<BookItem> items; // 该书的所有实体（副本）

    public Book() {}

    public Book(String name, String isbn, String author,
                String publisher, LocalDate publishDate,
                String description, int inventory, Category category,
                List<BookItem> items) {
        this.name = name;
        this.isbn = isbn;
        this.author = author;
        this.publisher = publisher;
        this.publishDate = publishDate;
        this.description = description;
        this.inventory = inventory;
        this.category = category;
        this.items = items;
    }

    // Getter 和 Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getInventory() {
        return inventory;
    }

    public void setInventory(int inventory) {
        this.inventory = inventory;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<BookItem> getItems() {
        return items;
    }

    public void setItems(List<BookItem> items) {
        this.items = items;
    }

    // toString 方法
    @Override
    public String toString() {
        return "Book{" +
                "name='" + name + '\'' +
                ", isbn='" + isbn + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publishDate=" + (publishDate != null ? publishDate : "未知") +
                ", description='" + description + '\'' +
                ", inventory=" + inventory +
                ", category=" + (category != null ? category.toString() : "未分类") +
                ", items=" + (items != null ? items.toString() : "无副本") +
                '}';
    }
}
