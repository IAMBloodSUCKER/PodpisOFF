package com.podpisoff.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class SettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String username = "settings_" + System.nanoTime();
        token = registerUser(username);
    }

    @Test
    void updateBillingReminderDays() throws Exception {
        mockMvc.perform(patch("/api/settings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billingReminderDaysBefore":7}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.billingReminderDaysBefore").value(7));

        mockMvc.perform(patch("/api/settings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billingReminderDaysBefore":0}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.billingReminderDaysBefore").value(0));
    }

    @Test
    void rejectsInvalidBillingReminderDays() throws Exception {
        mockMvc.perform(patch("/api/settings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billingReminderDaysBefore":2}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid billing reminder days"));
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
                      "locale":"en",
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

    private CaptchaChallenge issueCaptcha() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/captcha")).andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String question = json.get("question").asText();
        String expression = question.replace(" = ?", "").trim();
        String[] parts = expression.split(" \\+ ");
        int sum = Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
        return new CaptchaChallenge(json.get("captchaId").asText(), String.valueOf(sum));
    }

    private String bearer(String jwt) {
        return "Bearer " + jwt;
    }

    private record CaptchaChallenge(String id, String answer) {
    }
}
