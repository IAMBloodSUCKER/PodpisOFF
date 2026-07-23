package com.podpisoff.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.podpisoff.common.ApiException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class YandexOAuthClient {

    private static final String AUTHORIZE_URL = "https://oauth.yandex.ru/authorize";
    private static final String TOKEN_URL = "https://oauth.yandex.ru/token";
    private static final String INFO_URL = "https://login.yandex.ru/info";

    private final YandexOAuthProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YandexOAuthClient(YandexOAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String buildAuthorizeUrl(String state) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Yandex OAuth is not configured");
        }
        return AUTHORIZE_URL
            + "?response_type=code"
            + "&client_id=" + encode(properties.clientId())
            + "&redirect_uri=" + encode(properties.redirectUri())
            + "&scope=" + encode("login:info login:email")
            + "&force_confirm=yes"
            + "&state=" + encode(state);
    }

    public String exchangeCode(String code) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Yandex OAuth is not configured");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());

        try {
            String body = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
            JsonNode json = objectMapper.readTree(body == null ? "{}" : body);
            String accessToken = text(json, "access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Yandex token exchange failed");
            }
            return accessToken;
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientException | java.io.IOException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Yandex token exchange failed");
        }
    }

    public YandexUserInfo fetchUserInfo(String accessToken) {
        try {
            String body = restClient.get()
                .uri(INFO_URL + "?format=json")
                .header("Authorization", "OAuth " + accessToken)
                .retrieve()
                .body(String.class);
            JsonNode json = objectMapper.readTree(body == null ? "{}" : body);
            String id = text(json, "id");
            if (id == null || id.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Yandex user info is incomplete");
            }
            String login = text(json, "login");
            String email = text(json, "default_email");
            if ((email == null || email.isBlank()) && json.has("emails") && json.get("emails").isArray()
                && !json.get("emails").isEmpty()) {
                email = json.get("emails").get(0).asText(null);
            }
            String displayName = text(json, "display_name");
            if (displayName == null || displayName.isBlank()) {
                displayName = text(json, "real_name");
            }
            return new YandexUserInfo(id, login, email, displayName);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientException | java.io.IOException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Yandex user info request failed");
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record YandexUserInfo(String id, String login, String email, String displayName) {
    }
}
