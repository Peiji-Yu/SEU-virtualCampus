package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务器主类
 * 负责启动服务器并监听客户端连接
 */
public class Server {
    private static final int PORT = 8888;
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("智慧校园服务器启动，监听端口: " + PORT);

            // 循环接受客户端连接
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // 为每个客户端连接创建一个新的ClientHandler，并提交到线程池执行
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}
