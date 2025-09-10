package Server.dao.shop;

import Server.model.shop.StoreItem;
import Server.model.shop.StoreOrder;
import Server.model.shop.StoreOrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * 商店数据访问接口
 */
public interface StoreMapper {
    // 商品相关操作

    /**
     * 查询所有商品
     */
    @Select("SELECT * FROM store_item")
    List<StoreItem> findAllItems();

    /**
     * 根据关键词搜索商品
     */
    @Select("SELECT * FROM store_item WHERE item_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')")
    List<StoreItem> searchItems(@Param("keyword") String keyword);

    /**
     * 根据ID查询商品
     */
    @Select("SELECT * FROM store_item WHERE uuid = #{uuid}")
    StoreItem findItemById(@Param("uuid") UUID uuid);

    /**
     * 添加新商品
     */
    @Insert("INSERT INTO store_item (uuid, item_name, price, picture_link, stock, sales_volume, description, barcode) " +
            "VALUES (#{uuid}, #{itemName}, #{price}, #{pictureLink}, #{stock}, #{salesVolume}, #{description}, #{barcode})")
    int insertItem(StoreItem item);

    /**
     * 更新商品信息
     */
    @Update("UPDATE store_item SET item_name = #{itemName}, price = #{price}, picture_link = #{pictureLink}, " +
            "stock = #{stock}, sales_volume = #{salesVolume}, description = #{description}, barcode = #{barcode} " +
            "WHERE uuid = #{uuid}")
    int updateItem(StoreItem item);

    /**
     * 删除商品
     */
    @Delete("DELETE FROM store_item WHERE uuid = #{uuid}")
    int deleteItem(@Param("uuid") UUID uuid);

    /**
     * 更新商品库存
     */
    @Update("UPDATE store_item SET stock = stock - #{amount} WHERE uuid = #{itemUuid} AND stock >= #{amount}")
    int updateItemStock(@Param("itemUuid") UUID itemUuid, @Param("amount") Integer amount);

    /**
     * 增加商品销量
     */
    @Update("UPDATE store_item SET sales_volume = sales_volume + #{amount} WHERE uuid = #{itemUuid}")
    int increaseItemSales(@Param("itemUuid") UUID itemUuid, @Param("amount") Integer amount);

    /**
     * 按类别搜索商品
     */
    @Select("SELECT * FROM store_item WHERE category = #{category}")
    List<StoreItem> findItemsByCategory(@Param("category") String category);

    /**
     * 获取所有商品类别
     */
    @Select("SELECT DISTINCT category FROM store_item")
    List<String> findAllCategories();

    /**
     * 按类别和关键词搜索商品
     */
    @Select("SELECT * FROM store_item WHERE category = #{category} AND " +
            "(item_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    List<StoreItem> searchItemsByCategoryAndKeyword(@Param("category") String category,
                                                    @Param("keyword") String keyword);

    // 订单相关操作（支持多种商品）

    /**
     * 插入订单主信息
     */
    @Insert("INSERT INTO store_order (uuid, card_number, total_amount, time, status, remark) " +
            "VALUES (#{uuid}, #{cardNumber}, #{totalAmount}, #{time}, #{status}, #{remark})")
    int insertOrder(StoreOrder order);

    /**
     * 插入订单商品项
     */
    @Insert("INSERT INTO store_order_item (uuid, order_uuid, item_uuid, item_price, amount) " +
            "VALUES (#{uuid}, #{orderUuid}, #{itemUuid}, #{itemPrice}, #{amount})")
    int insertOrderItem(StoreOrderItem orderItem);

    /**
     * 根据ID查询订单
     */
    @Select("SELECT * FROM store_order WHERE uuid = #{uuid}")
    StoreOrder findOrderById(@Param("uuid") UUID uuid);

//    /**
//     * 查询订单的所有商品项
//     */
//    @Select("SELECT * FROM store_order_item WHERE order_uuid = #{orderUuid}")
//    List<StoreOrderItem> findOrderItemsByOrderId(@Param("orderUuid") UUID orderUuid);

    /**
     * 根据ID查询订单（包含商品项和商品详细信息）
     */
    @Select("SELECT so.*, soi.uuid as item_id, soi.item_uuid, soi.item_price, soi.amount, " +
            "si.item_name, si.picture_link, si.description, si.barcode " +
            "FROM store_order so " +
            "LEFT JOIN store_order_item soi ON so.uuid = soi.order_uuid " +
            "LEFT JOIN store_item si ON soi.item_uuid = si.uuid " +
            "WHERE so.uuid = #{uuid}")
    @Results({
            @Result(property = "uuid", column = "uuid"),
            @Result(property = "cardNumber", column = "card_number"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "time", column = "time"),
            @Result(property = "status", column = "status"),
            @Result(property = "remark", column = "remark"),
            @Result(property = "items", column = "uuid",
                    many = @Many(select = "Server.dao.shop.StoreMapper.findOrderItemsWithDetailsByOrderId"))
    })
    StoreOrder findOrderWithDetailsById(@Param("uuid") UUID uuid);

    /**
     * 查询订单的所有商品项（包含商品详细信息）
     */
    @Select("SELECT soi.*, si.item_name, si.picture_link, si.description, si.barcode " +
            "FROM store_order_item soi " +
            "LEFT JOIN store_item si ON soi.item_uuid = si.uuid " +
            "WHERE soi.order_uuid = #{orderUuid}")
    @Results({
            @Result(property = "uuid", column = "uuid"),
            @Result(property = "orderUuid", column = "order_uuid"),
            @Result(property = "itemUuid", column = "item_uuid"),
            @Result(property = "itemPrice", column = "item_price"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "item.itemName", column = "item_name"),
            @Result(property = "item.pictureLink", column = "picture_link"),
            @Result(property = "item.description", column = "description"),
            @Result(property = "item.category", column = "category"),
            @Result(property = "item.barcode", column = "barcode")
    })
    List<StoreOrderItem> findOrderItemsWithDetailsByOrderId(@Param("orderUuid") UUID orderUuid);

    /**
     * 更新订单状态
     */
    @Update("UPDATE store_order SET status = #{status} WHERE uuid = #{uuid}")
    int updateOrderStatus(@Param("uuid") UUID uuid, @Param("status") String status);

    /**
     * 删除订单（同时删除关联的商品项）
     */
    @Delete("DELETE FROM store_order WHERE uuid = #{uuid}")
    int deleteOrder(@Param("uuid") UUID uuid);

    @Delete("DELETE FROM store_order_item WHERE order_uuid = #{orderUuid}")
    int deleteOrderItems(@Param("orderUuid") UUID orderUuid);

    /**
     * 退款操作：更新订单状态为已退款
     */
    @Update("UPDATE store_order SET status = '已退款' WHERE uuid = #{orderUuid} AND status = '已支付'")
    int refundOrder(@Param("orderUuid") UUID orderUuid);

    /**
     * 减少商品销量（退款时调用）
     */
    @Update("UPDATE store_item SET sales_volume = sales_volume - #{amount} WHERE uuid = #{itemUuid}")
    int decreaseItemSales(@Param("itemUuid") UUID itemUuid, @Param("amount") Integer amount);

    /**
     * 查询用户的所有订单
     */
    @Select("SELECT * FROM store_transaction WHERE card_number = #{cardNumber} ORDER BY time DESC")
    List<StoreOrder> findOrdersByUser(@Param("cardNumber") Integer cardNumber);

    /**
     * 查询所有订单
     */
    @Select("SELECT * FROM store_transaction ORDER BY time DESC")
    List<StoreOrder> findAllOrders();

    // 销售统计相关操作

    /**
     * 获取商品销售统计
     */
    @Select("SELECT si.uuid, si.item_name, SUM(st.amount) as total_amount, SUM(st.item_price * st.amount) as total_revenue " +
            "FROM store_transaction st JOIN store_item si ON st.item_uuid = si.uuid " +
            "WHERE st.status = true " +
            "GROUP BY si.uuid, si.item_name " +
            "ORDER BY total_revenue DESC")
    List<SalesStats> getSalesStatistics();

    /**
     * 获取今日销售总额
     */
    @Select("SELECT SUM(item_price * amount) as total_revenue FROM store_transaction " +
            "WHERE status = true AND DATE(time) = CURDATE()")
    Integer getTodaySalesRevenue();

    // 销售统计结果映射类
    class SalesStats {
        private UUID itemUuid;
        private String itemName;
        private Integer totalAmount;
        private Integer totalRevenue;

        // Getter和Setter方法
        public UUID getItemUuid() {
            return itemUuid;
        }

        public void setItemUuid(UUID itemUuid) {
            this.itemUuid = itemUuid;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Integer totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Integer getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(Integer totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
    }
}
