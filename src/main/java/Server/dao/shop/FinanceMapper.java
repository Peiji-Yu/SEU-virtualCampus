package Server.dao.shop;

import Server.model.shop.CardTransaction;
import Server.model.shop.FinanceCard;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 一卡通数据访问接口
 */
public interface FinanceMapper {
    // 一卡通账户操作

    /**
     * 根据一卡通号查询账户信息
     */
    @Select("SELECT * FROM finance_card WHERE card_number = #{cardNumber}")
    FinanceCard findFinanceCardByCardNumber(@Param("cardNumber") Integer cardNumber);

    /**
     * 创建一卡通账户
     */
    @Insert("INSERT INTO finance_card (card_number, balance, status) VALUES (#{cardNumber}, #{balance}, #{status})")
    int insertFinanceCard(FinanceCard financeCard);

    /**
     * 更新一卡通余额
     */
    @Update("UPDATE finance_card SET balance = balance + #{amount} WHERE card_number = #{cardNumber}")
    int updateFinanceCardBalance(@Param("cardNumber") Integer cardNumber, @Param("amount") Integer amount);

    /**
     * 更新一卡通状态
     */
    @Update("UPDATE finance_card SET status = #{status} WHERE card_number = #{cardNumber}")
    int updateFinanceCardStatus(@Param("cardNumber") Integer cardNumber, @Param("status") String status);

    // 交易记录操作

    /**
     * 插入交易记录
     */
    @Insert("INSERT INTO card_transaction (uuid, card_number, amount, time, type, description, reference_id) " +
            "VALUES (#{uuid}, #{cardNumber}, #{amount}, #{time}, #{type}, #{description}, #{referenceId})")
    int insertCardTransaction(CardTransaction transaction);

    /**
     * 插入退款交易记录
     */
    @Insert("INSERT INTO card_transaction (uuid, card_number, amount, time, type, description, reference_id) " +
            "VALUES (#{uuid}, #{cardNumber}, #{amount}, #{time}, '退款', #{description}, #{referenceId})")
    int insertRefundTransaction(CardTransaction transaction);

    /**
     * 查询用户的交易记录
     */
    @Select("SELECT * FROM card_transaction WHERE card_number = #{cardNumber} ORDER BY time DESC")
    List<CardTransaction> findTransactionsByCardNumber(@Param("cardNumber") Integer cardNumber);

    /**
     * 根据交易类型查询交易记录
     */
    @Select("SELECT * FROM card_transaction WHERE card_number = #{cardNumber} AND type = #{type} ORDER BY time DESC")
    List<CardTransaction> findTransactionsByType(@Param("cardNumber") Integer cardNumber, @Param("type") String type);
}
