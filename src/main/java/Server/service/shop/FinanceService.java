package Server.service.shop;

import Server.dao.shop.FinanceMapper;
import Server.model.Response;
import Server.model.shop.CardTransaction;
import Server.model.shop.FinanceCard;
import Server.util.DatabaseUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

import static Server.model.shop.FinanceCard.STATUS_LOST;

/**
 * 一卡通服务类
 * 处理一卡通相关的业务逻辑
 */
public class FinanceService {
    /**
     * 挂失一卡通（用户自己操作）
     */
    public Response reportLoss(Integer cardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 检查一卡通是否存在
            FinanceCard card = financeMapper.findFinanceCardByCardNumber(cardNumber);
            if (card == null) {
                return Response.error("一卡通账户不存在");
            }

            // 检查是否已经是挂失状态
            if (FinanceCard.STATUS_LOST.equals(card.getStatus())) {
                return Response.error("一卡通已经是挂失状态");
            }

            // 更新状态为挂失
            int updateResult = financeMapper.updateFinanceCardStatus(cardNumber, FinanceCard.STATUS_LOST);

            sqlSession.commit();
            return updateResult > 0 ?
                    Response.success("挂失成功") :
                    Response.error("挂失失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "挂失过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 解除挂失一卡通（管理员操作）
     */
    public Response cancelReportLoss(Integer targetCardNumber) {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 检查目标一卡通是否存在
            FinanceCard card = financeMapper.findFinanceCardByCardNumber(targetCardNumber);
            if (card == null) {
                return Response.error("目标一卡通账户不存在");
            }

            // 检查是否不是挂失状态
            if (!FinanceCard.STATUS_LOST.equals(card.getStatus())) {
                return Response.error("一卡通不是挂失状态，无法解除挂失");
            }

            // 更新状态为正常
            int updateResult = financeMapper.updateFinanceCardStatus(targetCardNumber, FinanceCard.STATUS_NORMAL);

            sqlSession.commit();
            return updateResult > 0 ?
                    Response.success("解除挂失成功") :
                    Response.error("解除挂失失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "解除挂失过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 查询所有挂失的一卡通账号信息（管理员功能）
     */
    public Response findAllLostCards() {
        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 查询所有挂失的一卡通账号
            List<FinanceMapper.LostCardInfo> lostCards = financeMapper.findAllLostCards();

            return Response.success("查询挂失一卡通成功", lostCards);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(500, "查询挂失一卡通失败: " + e.getMessage());
        }
    }

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

            FinanceCard card = financeMapper.findFinanceCardByCardNumber(cardNumber);
            if (card == null) {
                throw new RuntimeException("一卡通账户不存在");
            }
            if (STATUS_LOST.equals(card.getStatus())) {
                throw new RuntimeException("一卡通已挂失，无法充值");
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
            if (STATUS_LOST.equals(card.getStatus())) {
                throw new RuntimeException("一卡通已挂失，无法充值");
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
     * 退款到一卡通账户
     */
    public boolean refundToFinanceCard(Integer cardNumber, Integer amount, String description, String referenceId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("退款金额必须大于0");
        }

        try (SqlSession sqlSession = DatabaseUtil.getSqlSession()) {
            FinanceMapper financeMapper = sqlSession.getMapper(FinanceMapper.class);

            // 检查账户是否存在
            FinanceCard card = financeMapper.findFinanceCardByCardNumber(cardNumber);
            if (card == null) {
                throw new RuntimeException("一卡通账户不存在");
            }

            // 更新余额（增加退款金额）
            int updateResult = financeMapper.updateFinanceCardBalance(cardNumber, amount);

            // 记录退款交易
            if (updateResult > 0) {
                CardTransaction transaction = new CardTransaction(
                        cardNumber, amount, "退款",
                        description != null ? description : "订单退款",
                        referenceId
                );
                financeMapper.insertRefundTransaction(transaction);
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
