package Client.library.util.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Book {
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private String isbn;
    private String description;
    private String category;
    private int totalCopies;
    private int availableCopies;
    private List<BookCopy> copies;

    public Book() {
        // 默认构造函数
    }

    // Getter和Setter方法
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public List<BookCopy> getCopies() { return copies; }
    public void setCopies(List<BookCopy> copies) { this.copies = copies; }

    // 图书副本内部类
    public static class BookCopy {
        private UUID copyId;
        private String location;
        private String status;

        public BookCopy() {
            // 默认构造函数
        }

        // Getter和Setter方法
        public UUID getCopyId() { return copyId; }
        public void setCopyId(UUID copyId) { this.copyId = copyId; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}