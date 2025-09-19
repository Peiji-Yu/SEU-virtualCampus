package Client.panel.finance;

import Client.ClientNetworkHelper;
import Client.model.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * 一卡通/交易管理请求发送封装。
 * 仅负责构建请求与发送，返回原始 JSON 字符串，解析由调用方处理。
 */
public final class FinanceRequestSender {
    private FinanceRequestSender() {}

    public static String getFinanceCard(int cardNumber) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        return ClientNetworkHelper.send(new Request("getFinanceCard", data));
    }

    public static String rechargeFinanceCard(int cardNumber, int amount, String description) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("amount", amount);
        data.put("description", description == null ? "" : description);
        return ClientNetworkHelper.send(new Request("rechargeFinanceCard", data));
    }

    public static String getTransactions(int cardNumber, String type) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        if (type != null && !type.isBlank()) {
            data.put("type", type);
        }
        return ClientNetworkHelper.send(new Request("getTransactions", data));
    }
}

