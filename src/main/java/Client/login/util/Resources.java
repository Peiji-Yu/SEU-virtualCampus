package Client.login.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 统一资源：字体/颜色。提供安全加载，保证常量不为 null。
 */
public class Resources {
    private static Font loadStream(String path,double size){
        try(InputStream is = Resources.class.getResourceAsStream(path)){
            if(is==null){
                System.err.println("[Resources] Font stream null: " + path);
                return Font.font(size);
            }
            byte[] bytes = is.readAllBytes();
            Font f = Font.loadFont(new ByteArrayInputStream(bytes), size);
            if(f==null){
                System.err.println("[Resources] Font load null after read, path="+path+" bytes="+bytes.length);
                return Font.font(size);
            }
            return f;
        } catch (IOException e){
            System.err.println("[Resources] Font load IO error: " + path + " -> " + e.getMessage());
            return Font.font(size);
        } catch (Exception e){
            System.err.println("[Resources] Font load error: " + path + " -> " + e.getMessage());
            return Font.font(size);
        }
    }
    public static final Font ROBOTO_BOLD = loadStream("/Font/Lobster-Regular.ttf",32);
    public static final Font ROBOTO_BOLD_LARGE = loadStream("/Font/Lobster-Regular.ttf",40);
    public static final Font ROBOTO_REGULAR = loadStream("/Font/Roboto-Regular-14.ttf",16);
    public static final Font ROBOTO_REGULAR_MIN = loadStream("/Font/Roboto-Regular-14.ttf",14);
    public static final Font ROBOTO_LIGHT = loadStream("/Font/Roboto-Light-10.ttf",12);
    public static final Font ICON_FONT_MIN = loadStream("/IconFont/iconfont.ttf",18);
    public static final Font ICON_FONT = loadStream("/IconFont/iconfont.ttf",22);

    public static final Color WHITE = Color.WHITE;
    public static final Color SECONDARY = Color.valueOf("#5BA3E7");
    public static final Color PRIMARY = Color.valueOf("#2890C8");
    public static final Color DISABLED = Color.valueOf("#B4C0C7");
    public static final Color FONT_COLOR = Color.valueOf("#4d5152ff");
}
