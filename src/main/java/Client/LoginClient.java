package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import Server.model.Request;
/**
 * 虚拟校园登录客户端
 */
public class LoginClient extends JFrame {
    private JTextField cardNumberField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    // 服务器配置
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    // 简单的JSON处理工具（替代Gson）
    private JSONUtil jsonUtil = new JSONUtil();

    public LoginClient() {
        setTitle("虚拟校园登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 标题
        JLabel titleLabel = new JLabel("虚拟校园系统登录", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 表单面板
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        JLabel cardNumberLabel = new JLabel("一卡通号:");
        cardNumberLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        cardNumberField = new JTextField();

        JLabel passwordLabel = new JLabel("密码:");
        passwordLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        passwordField = new JPasswordField();

        formPanel.add(cardNumberLabel);
        formPanel.add(cardNumberField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        // 占位符
        formPanel.add(new JLabel());
        loginButton = new JButton("登录");
        loginButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        formPanel.add(loginButton);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // 状态标签
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.BLUE);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);

        // 添加登录按钮事件监听
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });

        // 支持回车键登录
        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * 执行登录操作
     */
    private void performLogin() {
        String cardNumber = cardNumberField.getText().trim();
        String password = new String(passwordField.getPassword());

        // 输入验证
        if (cardNumber.isEmpty() || password.isEmpty()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("一卡通号和密码不能为空!");
            return;
        }

        // 禁用登录按钮，防止重复点击
        loginButton.setEnabled(false);
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setText("正在连接服务器...");

        // 使用后台线程执行网络操作，避免界面冻结
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 创建请求数据
                    Map data = new HashMap();
                    data.put("cardNumber", Integer.parseInt(cardNumber));
                    data.put("password", password);

                    Request request = new Request("login", data);

                    // 发送请求并获取响应
                    String response = sendRequestToServer(request);

                    // 在UI线程更新状态
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            handleServerResponse(response);
                        }
                    });

                } catch (final Exception ex) {
                    // 在UI线程显示错误
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            statusLabel.setForeground(Color.RED);
                            statusLabel.setText("登录失败: " + ex.getMessage());
                            loginButton.setEnabled(true);
                        }
                    });
                }
            }
        });
        networkThread.start();
    }

    /**
     * 向服务器发送请求并获取响应
     */
    private String sendRequestToServer(Request request) throws IOException {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            // 连接服务器
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 将请求对象转换为JSON字符串
            String jsonRequest = jsonUtil.toJson(request);
            byte[] jsonData = jsonRequest.getBytes("UTF-8");

            // 发送数据长度头（4字节）
            dos.writeInt(jsonData.length);

            // 发送JSON数据
            dos.write(jsonData);
            dos.flush();

            // 读取响应长度头
            int responseLength = dis.readInt();

            // 读取响应数据
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);

            return new String(responseData, "UTF-8");

        } finally {
            // 关闭资源
            try { if (dis != null) {
                dis.close();
            }
            } catch (IOException e) {}
            try { if (dos != null) {
                dos.close();
            }
            } catch (IOException e) {}
            try { if (socket != null) {
                socket.close();
            }
            } catch (IOException e) {}
        }
    }

    /**
     * 处理服务器响应
     */
    private void handleServerResponse(String response) {
        loginButton.setEnabled(true);

        try {
            // 简单的响应解析
            if (response.indexOf("success") != -1 || response.indexOf("true") != -1) {
                statusLabel.setForeground(Color.GREEN);
                statusLabel.setText("登录成功!");

                // 登录成功后可以打开主界面
                JOptionPane.showMessageDialog(this, "登录成功，欢迎使用虚拟校园系统!",
                        "成功", JOptionPane.INFORMATION_MESSAGE);

            } else if (response.indexOf("error") != -1 || response.indexOf("false") != -1) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("登录失败: 一卡通号或密码错误");
            } else {
                statusLabel.setForeground(Color.BLUE);
                statusLabel.setText("服务器响应: " + response);
            }
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("解析服务器响应时出错: " + e.getMessage());
        }
    }

    /**
     * 启动客户端
     */
    public static void main(String[] args) {
        // 设置UI风格
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启动登录界面
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginClient client = new LoginClient();
                client.setVisible(true);
            }
        });
    }
}


/**
 * 简单的JSON处理工具（替代Gson库）
 */
class JSONUtil {

    /**
     * 将Request对象转换为JSON字符串
     */
    public String toJson(Request request) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"").append(escapeJson(request.getType())).append("\"");

        if (request.getData() != null && !request.getData().isEmpty()) {
            json.append(",\"data\":{");
            boolean first = true;
            Map data = request.getData();
            for (Object key : data.keySet()) {
                if (!first) {
                    json.append(",");
                }
                Object value = data.get(key);
                json.append("\"").append(escapeJson(key.toString())).append("\":");

                if (value instanceof String) {
                    json.append("\"").append(escapeJson(value.toString())).append("\"");
                } else if (value instanceof Number) {
                    json.append(value);
                } else if (value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"").append(escapeJson(value.toString())).append("\"");
                }

                first = false;
            }
            json.append("}");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}