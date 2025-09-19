package Client.model.library;


/**
 * 书籍副本（馆藏实体类）
 */
public class BookItem {
    private String uuid;          // 每本书的唯一ID（条码）
    private String isbn;          // 对应 Book 的 ISBN
    private String place;         // 馆藏位置（书架/楼层）
    private BookStatus bookStatus; // 状态（在馆/借出/丢失）

    // 无参构造
    public BookItem() {}

    // 全参构造
    public BookItem(String uuid, String isbn, String place, BookStatus bookStatus) {
        this.uuid = uuid;
        this.isbn = isbn;
        this.place = place;
        this.bookStatus = bookStatus;
    }

    // Getter 和 Setter
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIsbn() {
        return isbn;
    }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPlace() {
        return place;
    }
    public void setPlace(String place) {
        this.place = place;
    }

    public BookStatus getBookStatus() {
        return bookStatus;
    }
    public void setBookStatus(String bookStatus) {
        this.bookStatus = BookStatus.valueOf(bookStatus);
    }
    public void setBookStatus(BookStatus bookStatus) {
        this.bookStatus = bookStatus;
    }

    // toString 方法
    @Override
    public String toString() {
        return "BookItem{" +
                "uuid='" + uuid + '\'' +
                ", isbn='" + isbn + '\'' +
                ", place='" + place + '\'' +
                ", bookStatus=" + (bookStatus != null ? bookStatus.toString() : "未知状态") +
                '}';
    }
}
