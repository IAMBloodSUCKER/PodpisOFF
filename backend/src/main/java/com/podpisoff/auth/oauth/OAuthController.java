package com.podpisoff.auth.oauth;

import com.podpisoff.auth.AuthResponse;
import com.podpisoff.common.ApiException;
import com.podpisoff.user.LocaleCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final YandexOAuthService yandexOAuthService;

    public OAuthController(YandexOAuthService yandexOAuthService) {
        this.yandexOAuthService = yandexOAuthService;
    }

    @GetMapping("/providers")
    public Map<String, Boolean> providers() {
        return Map.of("yandex", yandexOAuthService.isConfigured());
    }

    @GetMapping("/yandex/start")
    public void startYandex(@RequestParam(defaultValue = "RU") LocaleCode locale,
                            @RequestParam(defaultValue = "Europe/Moscow") String timezone,
                            @RequestParam(defaultValue = "false") boolean termsAccepted,
                            HttpServletResponse response) throws IOException {
        if (!yandexOAuthService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Yandex OAuth is not configured");
        }
        String url = yandexOAuthService.startAuthorizeUrl(locale, timezone, termsAccepted);
        response.sendRedirect(url);
    }

    @GetMapping("/yandex/callback")
    public void yandexCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error,
                               HttpServletResponse response) throws IOException {
        if (!yandexOAuthService.isConfigured()) {
            response.sendRedirect(yandexOAuthService.frontendErrorRedirect("oauth_unavailable"));
            return;
        }
        try {
            String redirect = yandexOAuthService.handleCallback(code, state, error);
            response.sendRedirect(redirect);
        } catch (ApiException ex) {
            String reason = "Terms must be accepted".equals(ex.getMessage()) ? "oauth_terms" : "oauth_failed";
            response.sendRedirect(yandexOAuthService.frontendErrorRedirect(reason));
        } catch (Exception ex) {
            response.sendRedirect(yandexOAuthService.frontendErrorRedirect("oauth_failed"));
        }
    }

    @GetMapping("/ticket")
    public AuthResponse exchangeTicket(@RequestParam String ticket) {
        return yandexOAuthService.exchangeTicket(ticket);
    }
}
