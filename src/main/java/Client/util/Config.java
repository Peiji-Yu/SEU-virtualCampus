package Client.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 简单配置读取占位类，避免缺失导致编译失败。
 * 约定从 classpath 下 application.properties 读取 key=value。
 */
public class Config {
    private static final Properties PROPS = new Properties();
    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) PROPS.load(in);
        } catch (IOException ignore) {}
    }
    public static String get(String key){ return PROPS.getProperty(key); }
}

