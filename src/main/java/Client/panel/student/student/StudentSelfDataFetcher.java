package Client.panel.student.student;

import Client.panel.student.service.StudentClientService;
import Client.model.student.Student;
import javafx.application.Platform;

/**
 * 学生个人数据异步获取 (self 包)。
 * 提供静态 fetch，根据一卡通号获取 Student 并通过回调返回结果。
 * 作者: @Msgo-srAm
 */
public final class StudentSelfDataFetcher {
    private StudentSelfDataFetcher() {}
    public interface Callback {void onSuccess(Student student); void onError(String message);}
    public static void fetch(String cardNumber, StudentClientService service, Callback cb) {
        new Thread(() -> {
            try {
                Student stu = service.getSelf(Integer.parseInt(cardNumber));
                Platform.runLater(() -> cb.onSuccess(stu));
            } catch (Exception e) {
                Platform.runLater(() -> cb.onError("网络连接失败: " + e.getMessage()));
            }
        }).start();
    }
}

