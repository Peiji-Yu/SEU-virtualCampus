package Server.service.shop;

import Server.dao.shop.StoreMapper;
import Server.model.shop.StoreItem;
import Server.model.shop.StoreOrder;
import Server.model.shop.StoreOrderItem;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.UUID;

import static Server.model.shop.StoreOrder.STATUS_PAID;
import static Server.model.shop.StoreOrder.STATUS_PENDING;

/**
 * 商店服务类
 * 处理商店相关的业务逻辑
 */
public class StoreService {

    /**
     * 获取所有商品
     */
    public List<StoreItem> getAllItems() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findAllItems();
        }
    }

    /**
     * 搜索商品
     */
    public List<StoreItem> searchItems(String keyword) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.searchItems(keyword);
        }
    }

    /**
     * 根据ID获取商品
     */
    public StoreItem getItemById(UUID uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findItemById(uuid);
        }
    }

    /**
     * 添加商品（管理员功能）
     */
    public boolean addItem(StoreItem item) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            int result = storeMapper.insertItem(item);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 更新商品信息（管理员功能）
     */
    public boolean updateItem(StoreItem item) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            int result = storeMapper.updateItem(item);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 删除商品（管理员功能）
     */
    public boolean deleteItem(UUID uuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            int result = storeMapper.deleteItem(uuid);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 按类别获取商品
     */
    public List<StoreItem> getItemsByCategory(String category) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findItemsByCategory(category);
        }
    }

    /**
     * 获取所有商品类别
     */
    public List<String> getAllCategories() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findAllCategories();
        }
    }

    /**
     * 按类别和关键词搜索商品
     */
    public List<StoreItem> searchItemsByCategoryAndKeyword(String category, String keyword) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.searchItemsByCategoryAndKeyword(category, keyword);
        }
    }

    /**
     * 创建订单（支持多种商品）
     */
    public StoreOrder createOrder(StoreOrder order) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);

            // 检查商品库存
            for (StoreOrderItem item : order.getItems()) {
                StoreItem storeItem = storeMapper.findItemById(item.getItemUuid());
                if (storeItem == null) {
                    throw new RuntimeException("商品不存在: " + item.getItemUuid());
                }

                if (storeItem.getStock() < item.getAmount()) {
                    throw new RuntimeException("商品库存不足: " + storeItem.getItemName());
                }
            }

            // 插入订单主信息
            int orderResult = storeMapper.insertOrder(order);
            if (orderResult == 0) {
                throw new RuntimeException("创建订单失败");
            }

            // 插入订单商品项
            for (StoreOrderItem item : order.getItems()) {
                item.setOrderUuid(order.getUuid());
                int itemResult = storeMapper.insertOrderItem(item);

                if (itemResult == 0) {
                    throw new RuntimeException("添加订单商品项失败");
                }

                // 更新商品库存
                int stockUpdateResult = storeMapper.updateItemStock(item.getItemUuid(), item.getAmount());
                if (stockUpdateResult == 0) {
                    throw new RuntimeException("更新商品库存失败");
                }
            }

            sqlSession.commit();
            return order;
        }
    }

    /**
     * 支付订单（使用一卡通支付）
     */
    public boolean payOrder(UUID orderUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            FinanceService financeService = new FinanceService();
            StoreOrder order = storeMapper.findOrderById(orderUuid);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 使用 equals 判断，避免字符串引用不一致导致失败
            if (!STATUS_PENDING.equals(order.getStatus())) {
                throw new RuntimeException("订单状态不正确: " + order.getStatus());
            }
            boolean paymentResult = financeService.consumeFinanceCard(
                    order.getCardNumber(),
                    order.getTotalAmount(),
                    "商店购物支付",
                    orderUuid.toString()
            );
            if (paymentResult) {
                int updateResult = storeMapper.updateOrderStatus(orderUuid, STATUS_PAID);
                for (StoreOrderItem item : order.getItems()) {
                    storeMapper.increaseItemSales(item.getItemUuid(), item.getAmount());
                }
                sqlSession.commit();
                return updateResult > 0;
            } else {
                throw new RuntimeException("支付失败");
            }
        }
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(UUID orderUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            StoreOrder order = storeMapper.findOrderById(orderUuid);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 使用 equals 判断
            if (STATUS_PAID.equals(order.getStatus())) {
                throw new RuntimeException("已支付的订单不能取消");
            }
            for (StoreOrderItem item : order.getItems()) {
                int stockUpdateResult = storeMapper.updateItemStock(item.getItemUuid(), -item.getAmount());
                if (stockUpdateResult == 0) {
                    throw new RuntimeException("更新商品库存失败");
                }
            }
            int result = storeMapper.deleteOrder(orderUuid);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 退款操作
     */
    public boolean refundOrder(UUID orderUuid, String refundReason) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            FinanceService financeService = new FinanceService();

            // 获取订单信息
            StoreOrder order = storeMapper.findOrderById(orderUuid);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            if (!StoreOrder.STATUS_PAID.equals(order.getStatus())) {
                throw new RuntimeException("只有已支付的订单才能退款");
            }

            // 将退款金额退回用户一卡通账户
            boolean refundResult = financeService.refundToFinanceCard(
                    order.getCardNumber(),
                    order.getTotalAmount(),
                    "订单退款: " + (refundReason != null ? refundReason : ""),
                    orderUuid.toString()
            );

            if (refundResult) {
                // 更新订单状态为已退款
                int updateResult = storeMapper.refundOrder(orderUuid);

                // 减少商品销量
                for (StoreOrderItem item : order.getItems()) {
                    storeMapper.decreaseItemSales(item.getItemUuid(), item.getAmount());
                }

                sqlSession.commit();
                return updateResult > 0;
            } else {
                throw new RuntimeException("退款失败");
            }
        }
    }

    /**
     * 获取用户订单
     */
    public List<StoreOrder> getUserOrders(Integer cardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findOrdersByUser(cardNumber);
        }
    }

    /**
     * 获取所有订单（管理员功能）
     */
    public List<StoreOrder> getAllOrders() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findAllOrders();
        }
    }

    // 添加获取完整订单信息的方法
    public StoreOrder getOrderById(UUID orderUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findOrderById(orderUuid);
        }
    }

    /**
     * 获取销售统计（管理员功能）
     */
    public List<StoreMapper.SalesStats> getSalesStatistics() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.getSalesStatistics();
        }
    }

    /**
     * 获取今日销售总额（管理员功能）
     */
    public Integer getTodaySalesRevenue() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.getTodaySalesRevenue();
        }
    }
}
