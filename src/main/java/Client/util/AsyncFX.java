package Client.util;

import javafx.application.Platform;

/**
 * JavaFX 辅助异步工具：在指定延迟后在 FX 线程执行任务。
 * 通用工具，供任意模块复用。
 * @author Msgo-srAm
 */
public final class AsyncFX {
    private AsyncFX() {}

    /**
     * 在指定毫秒延迟后，切回 FX 线程执行 runnable。
     */
    public static void runLaterDelay(long delayMillis, Runnable runnable) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(runnable);
        }, "fx-delay").start();
    }
}
