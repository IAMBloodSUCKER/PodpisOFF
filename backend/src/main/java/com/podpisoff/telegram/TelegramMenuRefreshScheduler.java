package com.podpisoff.telegram;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TelegramMenuRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelegramMenuRefreshScheduler.class);
    private static final long DEBOUNCE_MS = 400;

    private final ObjectProvider<TelegramBotMenuService> menuServiceProvider;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "telegram-menu-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

    public TelegramMenuRefreshScheduler(ObjectProvider<TelegramBotMenuService> menuServiceProvider) {
        this.menuServiceProvider = menuServiceProvider;
    }

    public void scheduleRefresh(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return;
        }
        pending.compute(chatId, (id, existing) -> {
            if (existing != null) {
                existing.cancel(false);
            }
            return scheduler.schedule(() -> {
                pending.remove(id);
                try {
                    menuServiceProvider.getObject().refreshMenuToBottom(id);
                } catch (Exception ex) {
                    log.debug("Failed to refresh Telegram menu for {}: {}", id, ex.getMessage());
                }
            }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        });
    }
}
