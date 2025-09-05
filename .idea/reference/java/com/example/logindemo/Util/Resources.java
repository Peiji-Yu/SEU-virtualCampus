package com.example.logindemo.Util;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Resources {
    public static final Font ROBOTO_BOLD = Font.loadFont(Resources.class.getResource("/Font/Roboto-Bold-3.ttf").toExternalForm(),26);
    public static final Font ROBOTO_REGULAR = Font.loadFont((Resources.class.getResource("/Font/Roboto-Regular-14.ttf").toExternalForm()),16);
    public static final Font ROBOTO_REGULAR_MIN = Font.loadFont((Resources.class.getResource("/Font/Roboto-Regular-14.ttf").toExternalForm()),14);
    public static final Font ROBOTO_LIGHT = Font.loadFont(Resources.class.getResource("/Font/Roboto-Light-10.ttf").toExternalForm(),12);
    public static final Font ICON_FONT_MIN = Font.loadFont(Resources.class.getResourceAsStream("/IconFont/iconfont.ttf"),18);
    public static final Font ICON_FONT = Font.loadFont(Resources.class.getResourceAsStream("/IconFont/iconfont.ttf"),22);

    public static final Color WHITE = Color.WHITE;
    public static final Color SECONDARY = Color.valueOf("#5BA3E7");
    public static final Color PRIMARY = Color.valueOf("#2890C8");
    public static final Color DISABLED = Color.valueOf("#B4C0C7");
    public static final Color FONT_COLOR = Color.valueOf("#2C2E2F");
}
