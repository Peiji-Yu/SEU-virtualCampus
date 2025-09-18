package Client.teacherclass.timetable;

import javafx.scene.layout.BorderPane;
import Client.timetable.TimetablePanel;

/**
 * 教师端-我的课表（复用学生课表样式，后续可扩展教师课表数据）
 */
public class TeacherTimetablePanel extends BorderPane {
    public TeacherTimetablePanel(String cardNumber) {
        // 复用学生课表样式，显式指定 userType 为 "teacher"，使 TimetablePanel 使用教师查询接口
        TimetablePanel panel = new TimetablePanel(cardNumber, "teacher");
        this.setCenter(panel);
    }
}
