package Client.panel.finance;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// Transaction model class remains the same
public class Transaction {
    private final StringProperty transactionId = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty amount = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty time = new SimpleStringProperty();

    public Transaction(String id, String tp, String amt, String desc, String time) {
        this.transactionId.set(id); this.type.set(tp); this.amount.set(amt); this.description.set(desc); this.time.set(time);
    }

    public StringProperty transactionIdProperty(){ return transactionId; }
    public StringProperty typeProperty(){ return type; }
    public StringProperty amountProperty(){ return amount; }
    public StringProperty descriptionProperty(){ return description; }
    public StringProperty timeProperty(){ return time; }
}
