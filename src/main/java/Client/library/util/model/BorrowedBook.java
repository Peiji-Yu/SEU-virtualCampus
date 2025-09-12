package Client.library.util.model;

import java.time.LocalDate;
import java.util.UUID;

public class BorrowedBook {
    private int recordId;
    private String bookTitle;
    private String author;
    private UUID copyId;  // 修改为 UUID 类型
    private String location;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private boolean canRenew;
    private int totalBorrowDays;

    public BorrowedBook() {
        // 默认构造函数
    }

    // Getter和Setter方法
    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public UUID getCopyId() { return copyId; }  // 修改为 UUID 类型
    public void setCopyId(UUID copyId) { this.copyId = copyId; }  // 修改为 UUID 类型

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isCanRenew() { return canRenew; }
    public void setCanRenew(boolean canRenew) { this.canRenew = canRenew; }

    public int getTotalBorrowDays() { return totalBorrowDays; }
    public void setTotalBorrowDays(int totalBorrowDays) { this.totalBorrowDays = totalBorrowDays; }
}