package Server.service;

import Server.dao.FinanceMapper;
import Server.model.shop.CardTransaction;
import Server.model.shop.FinanceCard;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.UUID;

/**
 * 一卡通服务类
 * 处理一卡通相关的业务逻辑
 */
public class FinanceService {

    /**
     * 查询一卡通余额
     */
    public FinanceCard getFinanceCard(Integer cardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);
            return financeMapper.findFinanceCardByCardNumber(cardNumber);
        }
    }

    /**
     * 创建一卡通账户
     */
    public boolean createFinanceCard(Integer cardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 检查是否已存在
            FinanceCard existingCard = financeMapper.findFinanceCardByCardNumber(cardNumber);
            if (existingCard != null) {
                return true; // 已存在，视为创建成功
            }

            // 创建新账户
            FinanceCard newCard = new FinanceCard(cardNumber, 0, "正常");
            int result = financeMapper.insertFinanceCard(newCard);
            sqlSession.commit();
            return result > 0;
        }
    }

    /**
     * 一卡通充值
     */
    public boolean rechargeFinanceCard(Integer cardNumber, Integer amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("充值金额必须大于0");
        }

        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 检查账户是否存在
            FinanceCard card = financeMapper.findFinanceCardByCardNumber(cardNumber);
            if (card == null) {
                // 如果账户不存在，先创建
                createFinanceCard(cardNumber);
            }

            // 更新余额
            int updateResult = financeMapper.updateFinanceCardBalance(cardNumber, amount);

            // 记录交易
            if (updateResult > 0) {
                CardTransaction transaction = new CardTransaction(
                        cardNumber, amount, "充值",
                        description != null ? description : "一卡通充值",
                        null
                );
                financeMapper.insertCardTransaction(transaction);
            }

            sqlSession.commit();
            return updateResult > 0;
        }
    }

    /**
     * 一卡通消费
     */
    public boolean consumeFinanceCard(Integer cardNumber, Integer amount, String description, String referenceId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("消费金额必须大于0");
        }

        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 检查账户余额
            FinanceCard card = financeMapper.findFinanceCardByCardNumber(cardNumber);
            if (card == null) {
                throw new RuntimeException("一卡通账户不存在");
            }

            if (card.getBalance() < amount) {
                throw new RuntimeException("一卡通余额不足");
            }

            // 更新余额（减去消费金额）
            int updateResult = financeMapper.updateFinanceCardBalance(cardNumber, -amount);

            // 记录交易
            if (updateResult > 0) {
                CardTransaction transaction = new CardTransaction(
                        cardNumber, -amount, "消费",
                        description != null ? description : "一卡通消费",
                        referenceId
                );
                financeMapper.insertCardTransaction(transaction);
            }

            sqlSession.commit();
            return updateResult > 0;
        }
    }

    /**
     * 查询交易记录
     */
    public List<CardTransaction> getTransactions(Integer cardNumber, String type) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            if (type != null && !type.isEmpty()) {
                return financeMapper.findTransactionsByType(cardNumber, type);
            } else {
                return financeMapper.findTransactionsByCardNumber(cardNumber);
            }
        }
    }
}
