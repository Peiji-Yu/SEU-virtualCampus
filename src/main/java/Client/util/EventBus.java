package Client.util;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very small application event bus for cross-panel notifications.
 * Listeners are run on the JavaFX Application Thread.
 */
public final class EventBus {
    private static final Map<String, List<Runnable>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static void addListener(String event, Runnable listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public static void removeListener(String event, Runnable listener) {
        List<Runnable> list = listeners.get(event);
        if (list != null) {
            list.remove(listener);
        }
    }

    public static void post(String event) {
        List<Runnable> list = listeners.get(event);
        if (list == null || list.isEmpty()) return;
        // run listeners on FX thread
        Platform.runLater(() -> {
            // copy to avoid concurrent modification
            List<Runnable> copy = new ArrayList<>(list);
            for (Runnable r : copy) {
                try { r.run(); } catch (Exception ignored) {}
            }
        });
    }
}

