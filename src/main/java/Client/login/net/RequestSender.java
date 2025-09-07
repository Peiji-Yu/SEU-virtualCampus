package Client.login.net;

import Client.ClientNetworkHelper;
import Server.model.Request;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一封装客户端登录/找回密码/重置密码的请求发送与数据构建。
 * @author Msgo-srAm
 */
public final class RequestSender {
    private RequestSender() {}

    public static String login(String cardNumber, String password) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", Integer.parseInt(cardNumber));
        data.put("password", password);
        return ClientNetworkHelper.send(new Request("login", data));
    }

    public static String forgetPwd(String cardNumber, String id) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", Integer.parseInt(cardNumber));
        data.put("id", id);
        return ClientNetworkHelper.send(new Request("forgetPwd", data));
    }

    public static String resetPwd(String cardNumber, String newPassword) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", Integer.parseInt(cardNumber));
        data.put("password", newPassword);
        return ClientNetworkHelper.send(new Request("resetPwd", data));
    }
}
