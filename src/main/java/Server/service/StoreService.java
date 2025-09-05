package Server.service;

import Server.dao.StoreMapper;
import Server.model.shop.StoreItem;
import Server.model.shop.StoreOrder;
import Server.model.shop.StoreOrderItem;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     * 创建订单（支持多种商品）
     */
    public StoreOrder createOrder(StoreOrder order) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            FinanceService financeService = new FinanceService();

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

            // 获取订单信息
            StoreOrder order = storeMapper.findOrderById(orderUuid);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            if (!"待支付".equals(order.getStatus())) {
                throw new RuntimeException("订单状态不正确: " + order.getStatus());
            }

            // 使用一卡通支付
            boolean paymentResult = financeService.consumeFinanceCard(
                    order.getCardNumber(),
                    order.getTotalAmount(),
                    "商店购物支付",
                    orderUuid.toString()
            );

            if (paymentResult) {
                // 更新订单状态为已支付
                int updateResult = storeMapper.updateOrderStatus(orderUuid, "已支付");

                // 增加商品销量
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

    // 添加获取完整订单信息的方法
    public StoreOrder getOrderById(UUID orderUuid) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            StoreMapper storeMapper = sqlSession.getMapper(StoreMapper.class);
            return storeMapper.findOrderById(orderUuid);
        }
    }
}
