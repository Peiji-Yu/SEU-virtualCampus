package Client.shop;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class StoreController implements Initializable {

    // 模拟商品数据
    private final ObservableList<StoreClient.StoreItem> items = FXCollections.observableArrayList(
            new StoreItem("1", "Java编程思想", 89.00, "/images/java_book.jpg", "经典的Java编程教材，深入讲解Java语言特性和编程思想"),
            new StoreItem("2", "Python编程", 75.50, "/images/python_book.jpg", "Python入门到精通，适合初学者和进阶开发者"),
            new StoreItem("3", "算法导论", 120.00, "/images/algorithms_book.jpg", "计算机算法经典教材，涵盖各种算法和数据结构"),
            new StoreItem("4", "设计模式", 68.00, "/images/design_patterns.jpg", "软件开发设计模式，提高代码质量和可维护性"),
            new StoreItem("5", "计算机网络", 85.00, "/images/network_book.jpg", "计算机网络原理，讲解网络协议和架构"),
            new StoreItem("6", "数据库系统", 79.00, "/images/database_book.jpg", "数据库系统概念，SQL和NoSQL数据库设计"),
            new StoreItem("7", "操作系统", 92.00, "/images/os_book.jpg", "操作系统原理，进程管理、内存管理和文件系统"),
            new StoreItem("8", "编译原理", 88.50, "/images/compiler_book.jpg", "编译原理与技术，词法分析、语法分析和代码生成")
    );

    // 购物车
    private final Map<String, CartItem> cart = new HashMap<>();

    // FXML注入的组件
    @FXML private VBox menuBar;
    @FXML private StackPane contentArea;
    @FXML private VBox storeContainer;
    @FXML private ScrollPane itemsContainer;
    @FXML private FlowPane itemsFlow;
    @FXML private VBox cartContainer;
    @FXML private VBox itemDetailContainer;
    @FXML private Button storeButton;
    @FXML private Button cartButton;
    @FXML private TextField searchField;

    // 当前视图与前一个视图
    private String currentView = "store";
    private String previousView = "store";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化商品显示
        updateItemsFlow(items);

        // 设置菜单按钮样式
        updateMenuSelection("store");

        // 设置菜单按钮的鼠标事件
        setupMenuButtonHover(storeButton);
        setupMenuButtonHover(cartButton);
    }

    private void setupMenuButtonHover(Button button) {
        button.setOnMouseEntered(e -> {
            if (!isButtonActive(button)) {
                button.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-font-size: 14px;");
            }
        });

        button.setOnMouseExited(e -> {
            if (!isButtonActive(button)) {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px;");
            }
        });
    }

    private boolean isButtonActive(Button button) {
        return (button == storeButton && "store".equals(currentView)) ||
                (button == cartButton && "cart".equals(currentView));
    }

    @FXML
    private void showStoreView() {
        hideAllViews();
        storeContainer.setVisible(true);
        updateMenuSelection("store");
        currentView = "store";
    }

    @FXML
    private void showCartView() {
        hideAllViews();
        updateCartView();
        cartContainer.setVisible(true);
        updateMenuSelection("cart");
        currentView = "cart";
    }

    @FXML
    private void filterItems() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.trim().isEmpty()) {
            updateItemsFlow(items);
        } else {
            ObservableList<StoreItem> filteredItems = FXCollections.observableArrayList();
            String lowerKeyword = keyword.toLowerCase();
            for (StoreItem item : items) {
                if (item.getName().toLowerCase().contains(lowerKeyword) ||
                        item.getDescription().toLowerCase().contains(lowerKeyword)) {
                    filteredItems.add(item);
                }
            }
            updateItemsFlow(filteredItems);
        }
    }

    // 其他方法（updateItemsFlow, createItemCard, addToCart等）保持与原来StoreClient中相同
    // ...

    // 注意：这里需要将原来StoreClient中的所有UI相关方法复制过来
}
