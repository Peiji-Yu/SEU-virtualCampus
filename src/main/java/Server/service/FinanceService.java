package Server.service;

import Server.dao.FinanceMapper;
import Server.model.shop.CardTransaction;
import Server.model.shop.FinanceCard;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

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
     * 一卡通充值
     */
    public boolean rechargeFinanceCard(Integer cardNumber, Integer amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("充值金额必须大于0");
        }

        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

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
