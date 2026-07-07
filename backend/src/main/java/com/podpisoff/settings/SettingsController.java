package com.podpisoff.settings;

import com.podpisoff.auth.AuthResponse;
import com.podpisoff.telegram.TelegramLinkResponse;
import com.podpisoff.telegram.TelegramLinkService;
import com.podpisoff.telegram.TelegramLinkStatusResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final TelegramLinkService telegramLinkService;

    public SettingsController(SettingsService settingsService, TelegramLinkService telegramLinkService) {
        this.settingsService = settingsService;
        this.telegramLinkService = telegramLinkService;
    }

    @GetMapping("/notification-channels")
    public NotificationChannelsResponse notificationChannels() {
        return settingsService.notificationChannels();
    }

    @PatchMapping
    public AuthResponse update(@RequestBody @Valid SettingsUpdateRequest request) {
        return settingsService.update(request);
    }

    @PostMapping("/telegram/link")
    public TelegramLinkResponse createTelegramLink() {
        return telegramLinkService.createLink();
    }

    @GetMapping("/telegram/link-status")
    public TelegramLinkStatusResponse telegramLinkStatus() {
        return telegramLinkService.linkStatus();
    }

    @DeleteMapping("/telegram/link")
    public AuthResponse disconnectTelegram() {
        telegramLinkService.disconnect();
        return settingsService.currentUser();
    }
}
