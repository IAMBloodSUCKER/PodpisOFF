package com.podpisoff.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFeedbackIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String username = "fbuser_" + System.nanoTime();
        userToken = registerUser(username);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"password123"}
                    """.formatted(username)))
            .andExpect(status().isOk());
    }

    @Test
    void feedbackReplyCreatesNotification() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Отличное приложение, спасибо!\"}"))
            .andExpect(status().isOk());

        String feedbackList = mockMvc.perform(get("/api/admin/feedback")
                .header("Authorization", bearer(userToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        long feedbackId = objectMapper.readTree(feedbackList).get(0).get("id").asLong();

        mockMvc.perform(post("/api/admin/feedback/" + feedbackId + "/reply")
                .header("Authorization", bearer(userToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reply\":\"Спасибо за отзыв!\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Ответ на ваш отзыв"))
            .andExpect(jsonPath("$[0].body").value("Спасибо за отзыв!"));

        mockMvc.perform(post("/api/admin/feedback/" + feedbackId + "/reply")
                .header("Authorization", bearer(userToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reply\":\"Повторный ответ\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testNotificationCreatesBellEntry() throws Exception {
        mockMvc.perform(post("/api/admin/test-notification")
                .header("Authorization", bearer(userToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"delaySeconds\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delaySeconds").value(0));

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Тестовое уведомление"));
    }

    @Test
    void testNotificationRejectsInvalidDelay() throws Exception {
        mockMvc.perform(post("/api/admin/test-notification")
                .header("Authorization", bearer(userToken))
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"delaySeconds\":3}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminRequiresKey() throws Exception {
        mockMvc.perform(get("/api/admin/metrics").header("Authorization", bearer(userToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminMetricsAvailableWithKey() throws Exception {
        mockMvc.perform(get("/api/admin/metrics")
                .header("Authorization", bearer(userToken))
                .header("X-Admin-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").isNumber())
            .andExpect(jsonPath("$.loginsToday").value(2));
    }

    private String registerUser(String username) throws Exception {
        CaptchaChallenge captcha = issueCaptcha();
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"password123",
                      "timezone":"UTC",
                      "locale":"ru",
                      "termsAccepted": true,
                      "captchaId":"%s",
                      "captchaAnswer":"%s"
                    }
                    """.formatted(username, captcha.id(), captcha.answer())))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("auth").get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private CaptchaChallenge issueCaptcha() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/captcha")).andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String question = json.get("question").asText();
        String expression = question.replace(" = ?", "").trim();
        String[] parts = expression.split(" \\+ ");
        int sum = Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        return new CaptchaChallenge(json.get("captchaId").asText(), String.valueOf(sum));
    }

    private record CaptchaChallenge(String id, String answer) {
    }
}
